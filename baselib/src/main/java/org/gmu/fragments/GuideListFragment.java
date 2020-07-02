package org.gmu.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.gmu.base.GmuFragmentActivity;
import org.gmu.base.GmuMainActivity;
import org.gmu.base.R;
import org.gmu.adapters.place.GuidePlaceAdapter;
import org.gmu.adapters.place.PlaceAdapter;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.pojo.PlaceElement;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ttg
 * Date: 21/01/13
 * Time: 13:01
 * To change this template use File | Settings | File Templates.
 */
public class GuideListFragment extends PlacesListFragment
{

    private TextView actionbuttonText = null;
    private static final String TAG = GuideListFragment.class.toString();
    protected PlaceAdapter createListAdapter() {
        List<PlaceElement> list = new ArrayList<PlaceElement>();
        GuidePlaceAdapter adapter = new GuidePlaceAdapter(getActivity(),
                android.R.layout.simple_list_item_1, list);
        return adapter;
    }

    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placelist,
                container, false);
        LinearLayout actionbutton = (LinearLayout) view.findViewById(R.id.actionbutton);
        actionbuttonText = (TextView) view.findViewById(R.id.actionbutton_text);
        setButtonText();
        actionbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GuideListFragment.this.switchDownloadedFilter();
            }
        });

        view.findViewById(R.id.actionarea).setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        Log.d(TAG, "Fragment destroyed:" + this);
        if(Controller.getInstance().getConfig().getPurchaseManager()!=null) Controller.getInstance().getConfig().getPurchaseManager().dispose();
    }

    @Override
    public void refresh()
    {
        //Log.d("EO","Refresh list ");


        //always search on max deep
        int searchDeep = 999;
        //only search guides
        List<PlaceElement> elems = Controller.getInstance().getDao().list(Controller.getInstance().getFilter(),
                Controller.getInstance().getGroupFilter(),
                IPlaceElementDAO.GUIDE_ITEMS, searchDeep, Controller.getInstance().getDistanceOrderDefinition());
        this.removeNotVisibleElements(elems,Controller.getInstance().getGroupFilter());


        if (Controller.getInstance().isOnlyDownloadedGuidesView() && elems.size() == 0)
        {     //no downloaded files found switch filter and search

            boolean downloadedViewMode = Controller.getInstance().isOnlyDownloadedGuidesView();
            Controller.getInstance().getGmuContext().setOnlyGuidesDownloadedFilter(!downloadedViewMode);
            //refresh
            setButtonText();
            elems = Controller.getInstance().getDao().list(Controller.getInstance().getFilter(),
                    Controller.getInstance().getGroupFilter(),
                    IPlaceElementDAO.GUIDE_ITEMS, searchDeep, Controller.getInstance().getDistanceOrderDefinition());
            this.removeNotVisibleElements(elems,Controller.getInstance().getGroupFilter());
            //refresh toolbar to show guide filters
            ((GmuFragmentActivity)this.getActivity()).refreshNavigationBar();


        }
        if ((!Controller.getInstance().isOnlyDownloadedGuidesView()) && elems.size() == 0)
        {
            Toast.makeText(this.getActivity().getApplicationContext(),R.string.no_available, Toast.LENGTH_SHORT).show();
        }
        renderList(elems);


    }

    public static List<PlaceElement> getDownloadedGuides() {
        PlaceElement filter = new PlaceElement();
        filter.getAttributes().put("guidedownloaded", "TRUE");
        List<PlaceElement> elems = Controller.getInstance().getDao().list(filter,
                null,
                IPlaceElementDAO.GUIDE_ITEMS, 999, Controller.getInstance().getDistanceOrderDefinition());

        return elems;
    }

    private void switchDownloadedFilter() {    //invert filter
        final boolean downloadedViewMode = Controller.getInstance().isOnlyDownloadedGuidesView();
        if (!downloadedViewMode) {   //check if downloaded guides
            List<PlaceElement> elems = getDownloadedGuides();
            if (elems.size() == 0) {   //Toast and don't do nothing
                Toast.makeText(this.getActivity().getApplicationContext(),R.string.no_downloaded, Toast.LENGTH_SHORT).show();
                return;
            }

        }
        //toggle button text

        Controller.getInstance().getGmuContext().setOnlyGuidesDownloadedFilter(!downloadedViewMode);

        //refresh
        setButtonText();


        if (downloadedViewMode)
        {

                //search new guides on server
             ((GmuMainActivity) GuideListFragment.this.getActivity()).updateMainGuide();

        } else
        {       //remove filter and refresh
            Controller.getInstance().setGroupFilter(null);
            Controller.getInstance().refresh();
        }


    }

    private void setButtonText() {
        if (Controller.getInstance().isOnlyDownloadedGuidesView()) {
            actionbuttonText.setText(R.string.downloadmore);
        } else {
            actionbuttonText.setText(R.string.showdownloaded);
        }
    }

    @Override
    protected void removeNotVisibleElements(List<PlaceElement> result,String parentUid) {
        return;
    }

    @Override
    protected void onElementLongClick(String uid, View v) {
        PlaceElement elem = Controller.getInstance().getDao().load(uid);
        if (elem.getAttributes().get("guidedownloaded") != null
                && elem.getAttributes().get("guidedownloaded").equalsIgnoreCase("TRUE")) {
            ((GmuFragmentActivity) this.getActivity()).openItemActions(uid, v);
        }

    }




}
