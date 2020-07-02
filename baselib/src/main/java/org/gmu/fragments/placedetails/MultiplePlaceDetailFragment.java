package org.gmu.fragments.placedetails;


import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.viewpager.widget.ViewPager;

import com.viewpagerindicator.PageIndicator;
import org.gmu.base.GmuFragmentActivity;
import org.gmu.base.R;
import org.gmu.adapters.DetailPagerAdapter;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.ui.GmuViewPager;
import org.gmu.utils.Utils;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class MultiplePlaceDetailFragment extends AbstractPlaceDetailFragment
{
    private static final String TAG = MultiplePlaceDetailFragment.class.getName();

    private DetailPagerAdapter adapter;
    private GmuViewPager detailPager;
    private PageIndicator mIndicator;
    private String selectedUID = null;
    private Class type = GenericPlaceDetailFragment.class;
    protected ArrayList<String> UIDs = new ArrayList<String>();


    public MultiplePlaceDetailFragment() {
        super();

    }


    public void setUIDs(List<PlaceElement> relatedObjects) {
        this.UIDs = new ArrayList<String>();
        for (int i = 0; i < relatedObjects.size(); i++) {
            PlaceElement element = relatedObjects.get(i);
            this.UIDs.add(element.getUid());
        }


    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(false);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {


        super.onSaveInstanceState(outState);
        outState.putString("uids", Utils.objectToString(UIDs));
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (UIDs.size() == 0) {
            try {
                UIDs = (ArrayList<String>) Utils.stringToObject(savedInstanceState.getString("uids"));
            } catch (Exception ign) {
            }
        }

        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_multipledetailview,
                container, false);


        setupPager(main);
        return main;
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
        adapter.clear();
        System.gc();
        Runtime.getRuntime().gc();
        Log.d(TAG, "Destroyed");

    }


    public void onResume()
    {
        super.onResume();
        if (this.UID != null)
        {
             //slide between place children
                //obtain uids from related uids
               List<PlaceElement> p=  Controller.getInstance().getDao().getRelatedPlaceElements(UID, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, IPlaceElementDAO.PLACE_RELATED_ITEMS);
                p.add(0, Controller.getInstance().getDao().load(UID));


            this.setUIDs(p);

        }

        //TODO: refresh on resume (no hay manera de hacer que se refresque al restaurar instancia con el pager)!!
        showElements();
    }

    public void setType(Class type) {
        this.type = type;
    }

    private void setupPager(View main) {


        adapter = new DetailPagerAdapter(this.getChildFragmentManager());
        adapter.setType(type);
        detailPager = ((GmuViewPager) main.findViewById(R.id.detailPager));
        detailPager.setAdapter(adapter);


        mIndicator = (PageIndicator) main.findViewById(R.id.indicator);
        mIndicator.setViewPager(detailPager);

        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrolled(int i, float v, int i2)
            {

            }

            public void onPageSelected(int i)
            {    onPlaceSelected(adapter.getPlaceElementAt(i));


            }

            public void onPageScrollStateChanged(int i)
            {

            }
        });


    }


    protected void onPlaceSelected(PlaceElement place)
    {

        if (Utils.contains(IPlaceElementDAO.PLACE_RELATED_ITEMS, place.getType()))
        { //select child UID
            Controller.getInstance().setSelectedChild(place.getUid());
            selectedUID = Controller.getInstance().getSelectedChild();
        } else
        {
            //unselect child
            Controller.getInstance().setSelectedChild(null);
            Controller.getInstance().setSelectedUID(place.getUid());
            selectedUID = place.getUid();
            Controller.getInstance().getGmuContext().selectedListUID=selectedUID;
        }

        ((GmuFragmentActivity) MultiplePlaceDetailFragment.this.getActivity()).dealDetailPageRefresh();


    }


    protected void showElements() {
        //Log.d("EEO", "Show detail starts ");


        int selected = 0;
        selectedUID = Controller.getInstance().getSelectedChild();
        if(Utils.isEmpty(selectedUID))
        {
            selectedUID=Controller.getInstance().getSelectedUID();
        }
        adapter.clear();


        //add related children if found

        for (int i = 0; i < UIDs.size(); i++)
        {
            String s = UIDs.get(i);
            adapter.addPlaceElement(Controller.getInstance().getDao().load(s));

            if (Utils.equals(selectedUID, s))
            {   //center on child
                selected = i;

            }

        }

        detailPager.setCurrentItem(selected);

        adapter.notifyDataSetChanged();

        mIndicator.setViewPager(detailPager, selected);

        mIndicator.notifyDataSetChanged();

        mIndicator.setCurrentItem(selected);


    }








}
