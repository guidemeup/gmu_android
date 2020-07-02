package org.gmu.fragments;

import android.os.Bundle;
;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.fragment.app.ListFragment;

import org.gmu.base.GmuFragmentActivity;

import org.gmu.adapters.place.PlaceAdapter;
import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 8/11/12
 * Time: 17:29
 * To change this template use File | Settings | File Templates.
 */
public class PlacesListFragment extends ListFragment {
    private static final boolean MIXPARENTSANDCHILDREN = true;
    private static final String TAG = PlacesListFragment.class.toString();

    protected PlaceAdapter createListAdapter() {
        List<PlaceElement> list = new ArrayList<PlaceElement>();
        PlaceAdapter adapter = new PlaceAdapter(getActivity(),
                android.R.layout.simple_list_item_1, list);
        return adapter;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placelist,
                container, false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment destroyed:" + this);

    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        setListAdapter(this.createListAdapter());


    }

    public void onResume() {
        super.onResume();
        ((GmuFragmentActivity) this.getActivity()).onFragmentResumed(this);
        this.refresh();

    }


    protected void renderList(List<PlaceElement> result)
    {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                onElementClick(((PlaceElement) getListAdapter().getItem(position)).getUid());


            }
        });



        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onElementLongClick(((PlaceElement) getListAdapter().getItem(position)).getUid(), view);
                return true;
            }
        });

        ArrayAdapter listAdapter = (ArrayAdapter) getListAdapter();
        listAdapter.clear();
        boolean someGroup = false;

        int selected=0;
        int count=0;

        for (PlaceElement cart : result)

        {
            boolean isParent = Controller.getInstance().getDao().isParentPlaceElement(cart);
            if (!someGroup) {
                someGroup = isParent;
            }
            if (MIXPARENTSANDCHILDREN || !someGroup || isParent) {

                listAdapter.add(cart);

            }
            if(Controller.getInstance().getGmuContext().selectedListUID!=null
                    &&cart.getUid().equals(Controller.getInstance().getGmuContext().selectedListUID))
            {
               //fix selected
                selected=count;
            }
            count++;
        }


        if(!Utils.isEmpty(Controller.getInstance().getGmuContext().selectedListUID))
        {   //recover selection on back page


            //this.getListView().smoothScrollToPosition(selected);
            this.setSelection(selected);
            //this.getListView().getChildAt(selected).setBackgroundResource(R.drawable.boton_verde);

        }




    }

    protected void removeNotVisibleElements(List<PlaceElement> result,String parentId) {
        if (result.size() > 0) {
            PlaceElement parent = result.get(0);
            if(Utils.equals(parentId,parent.getUid()))
            {

                if (parent.getType().equals(PlaceElement.TYPE_GUIDE) || Utils.isEmpty(parent.getAttributes().get("description"))) {   //only show parent if has description
                    result.remove(parent);
                }
            }
        }
    }


    public void refresh()
    {



        List<PlaceElement> elems = Controller.getInstance().getFilteredItems();

        this.removeNotVisibleElements(elems,Controller.getInstance().getGroupFilter());
        renderList(elems);


    }

    protected void onElementClick(String uid) {
        //store selected element to move list on list return
        Controller.getInstance().getGmuContext().selectedListUID=uid;
        //deal element click detail
        Controller.getInstance().goToDetail(uid);
    }


    protected void onElementLongClick(String uid, View v) {

    }


}
