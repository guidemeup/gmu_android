package org.gmu.fragments.placedetails;


import android.content.Context;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.gmu.base.R;
import org.gmu.adapters.place.GuidePlaceAdapter;
import org.gmu.adapters.place.PlaceAdapter;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.fragments.AbstractMapFragment;
import org.gmu.fragments.maps.MapsForgeMapFragment;
import org.gmu.inappbilling.PurchaseItem;
import org.gmu.inappbilling.PurchaseListener;
import org.gmu.pojo.PlaceElement;
import org.gmu.track.Tracker;
import org.gmu.utils.Utils;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class ParentPlaceDetailFragment extends AbstractPlaceDetailFragment implements PurchaseListener {
    private static final String TAG = ParentPlaceDetailFragment.class.getName();
    private LinearLayout mHeaderView;
    private List<PlaceElement> relatedContents = null;


    private String title;
    private TabHost tabHost;
    private PlaceElement placeElement;

    public ParentPlaceDetailFragment() {
        super();

    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(false);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {


        destroyFragments();
        super.onSaveInstanceState(outState);
        outState.putString("uid", UID);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        resume(savedInstanceState);
        Log.d(TAG, "Create detail of:" + UID);
        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_detailview,
                container, false);

        mHeaderView = (LinearLayout) main.findViewById(R.id.header);


        return main;
    }


    @Override
    public void onDestroyView() {
        destroyFragments();
        super.onDestroyView();
        mHeaderView = null;
        System.gc();
        Runtime.getRuntime().gc();
        Log.d(TAG, "Destroyed detail of:" + this.getTitle());

    }


    private void initTabHost() {
        tabHost = (TabHost) this.getView().findViewById(R.id.tabHost);

        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("infotab").setIndicator(
                null, getResources().getDrawable(R.drawable.action_about_dark)).setContent(R.id.tab1));
//
//        tabHost.addTab(tabHost.newTabSpec("maptab").setIndicator(
//                null, getResources().getDrawable(R.drawable.location_map_dark)).setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("mmtab").setIndicator(
                null, getResources().getDrawable(R.drawable.content_picture_dark)).setContent(R.id.tab1));

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                refreshTab();
            }
        });

        //set font
        Controller.getInstance().setFont(tabHost, null, null);


    }

    private void refreshTab() {
        boolean added = false;

        String currentTab = null;
        if (tabHost != null) {
            currentTab = tabHost.getCurrentTabTag();
        }
        Log.d(TAG, "Tab is=" + currentTab);
        Fragment fr = null;
        if (currentTab == null || currentTab.equals("infotab")) {

            GenericPlaceDetailFragment det = new GenericPlaceDetailFragment();

            det.setUID(UID);
            det.showTitle(false);
            det.setImageRatio(Constants.HEIGHT_GUIDE_DETAIL_RATIO);
            //det.showMainImg(false);
            fr = det;
        } else if (currentTab.equals("maptab")) {
            AbstractMapFragment map = new MapsForgeMapFragment();
            fr = map;
        } else if (currentTab.equals("mmtab")) {
            MultiplePlaceDetailFragment mm = new MultiplePlaceDetailFragment();

            if (relatedContents != null && relatedContents.size() > 0) {
                mm.setUIDs(relatedContents);
                mm.setType(GenericPlaceDetailFragment.class);
            }

            fr = mm;

        }
        FragmentTransaction tr = this.getChildFragmentManager().beginTransaction();
        if (this.getChildFragmentManager().findFragmentByTag(TAG) != null) {
            tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            tr.replace(R.id.content, fr, TAG);
        } else {
            tr.add(R.id.content, fr, TAG);
            added = true;

        }
        tr.commit();

    }

    public void destroyFragments() {
        try {
            FragmentTransaction mTransaction = this.getChildFragmentManager().beginTransaction();

            Fragment fr = this.getChildFragmentManager().findFragmentByTag(TAG);
            if (fr != null) {
                mTransaction.remove(fr);
            }

            mTransaction.commit();
        } catch (Exception ign) {
            Log.w(TAG, "error remove", ign);
        }
    }

    public void onResume() {
        super.onResume();
        //TODO: refresh on resume (no hay manera de hacer que se refresque al restaurar instancia con el pager)!!
        show(UID);
    }


    public String getTitle() {
        return title;
    }


    private void show(String UID) {
        //check guides billing status
        if (Controller.getInstance().getConfig().getPurchaseManager() != null)
        {


            List<String> purchaseQuery = new LinkedList<String>();
            purchaseQuery.add(UID);
            Controller.getInstance().getConfig().getPurchaseManager().queryItems(purchaseQuery, this, this.getActivity());

        }

        Log.d(TAG, "Search " + UID);
        placeElement = Controller.getInstance().getDao().load(UID);
        if (placeElement == null) {
            Log.d(TAG, "Show uid null?" + UID);
            return;
        }
        relatedContents = Controller.getInstance().getDao().getRelatedPlaceElements(UID, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, IPlaceElementDAO.PLACE_RELATED_ITEMS);


        if (placeElement.getType().equals(PlaceElement.TYPE_GUIDE) &&
                !Controller.getInstance().getDao().getBaseGuideId().equals(placeElement.getUid())) {

            //show header
            LayoutInflater inflater = (LayoutInflater) this.getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.guidelist_row, null);
            mHeaderView.addView(v);

            //link button action

            GuidePlaceAdapter.fillPlaceRow(v, placeElement, getActivity().getBaseContext(), true);
            refreshGuideAccessButton(false);

        } else {
            PlaceAdapter.fillPlaceRow(mHeaderView, placeElement, getActivity().getBaseContext(), Controller.getInstance().getConfig().getShowDistance());

        }
        title = placeElement.getTitle();


        //TODO: porque no muestra el 0 inicialmente!?
        refreshTab();
        if (tabHost == null) {
            initTabHost();
        }
        //deal related content tab visibility
        if (relatedContents != null && relatedContents.size() > 0) {
            tabHost.getTabWidget().setVisibility(View.VISIBLE);
            //tabHost.getTabWidget().getChildAt(RELATED_ITEMS_TAB).setVisibility(View.VISIBLE);
        } else {
            tabHost.getTabWidget().setVisibility(View.GONE);
            //tabHost.getTabWidget().getChildAt(RELATED_ITEMS_TAB).setVisibility(View.GONE);

        }

    }

    private void refreshGuideAccessButton(boolean shakeButton) {

        //avoid errors when view is destroyed
        if(mHeaderView==null) return;
        //link button action
        LinearLayout accessButton = (LinearLayout) mHeaderView.findViewById(R.id.access);


        accessButton.setVisibility(View.VISIBLE);
        accessButton.setEnabled(false);
        if (Controller.getInstance().getDao().paymentNeeded(placeElement)) {


            if (Utils.isEmpty(placeElement.getAttributes().get("price"))) {

                accessButton.setEnabled(false);
            } else
            {

                accessButton.setEnabled(true);
                accessButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                       if(Controller.getInstance().getConfig().getPurchaseManager()!=null)
                       {
                           Controller.getInstance().getConfig().getPurchaseManager().startPurchase(placeElement.getUid(),
                                   ParentPlaceDetailFragment.this, ParentPlaceDetailFragment.this.getActivity());
                       }


                    }


                });
            }
        } else {
            accessButton.setEnabled(true);

            accessButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onAccessToGuideClick(placeElement.getUid());
                }
            });
        }
        GuidePlaceAdapter.updateAccessGuideText((TextView) mHeaderView.findViewById(R.id.access_text), placeElement);
        if(shakeButton)
        {
            Animation shake = AnimationUtils.loadAnimation(this.getActivity(), R.anim.shake);
            accessButton.startAnimation(shake);
        }


    }


    public void onPurchaseEnds(PurchaseItem purchasedItem, int resultCode) {
        if (resultCode == PurchaseListener.RESULT_OK) {
            List<PlaceElement> toSave = new LinkedList<PlaceElement>();

            PlaceElement p = Controller.getInstance().getDao().load(purchasedItem.id);

            p.getAttributes().put("purchased", "" + purchasedItem.purchased);
            toSave.add(p);


            Controller.getInstance().getDao().saveOrUpdate(toSave);
            LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            params.put("uid",placeElement.getUid());
            params.put("name",placeElement.getTitle());
            params.put("price",""+placeElement.getAttributes().get("price"));
            Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "purchase_guide", params);


            this.refreshGuideAccessButton(true);




        } else {   //TODO: traducir
            Log.w(TAG, "Warning: error on purchase");
            Toast.makeText(this.getActivity(), this.getActivity().getString(R.string.errorupdateprompt), Toast.LENGTH_LONG).show();
        }
    }

    public void onQueryEnds(Map<String, PurchaseItem> result, int resultCode) {
        if (resultCode == PurchaseListener.RESULT_OK)
        {   boolean shake=false;
            //update in bdd
            Collection<PurchaseItem> it = result.values();
            List<PlaceElement> toSave = new LinkedList<PlaceElement>();
            for (Iterator<PurchaseItem> iterator = it.iterator(); iterator.hasNext(); ) {
                PurchaseItem next = iterator.next();
                PlaceElement p = Controller.getInstance().getDao().load(next.id);


                if (p != null)
                {   String initialState=p.getAttributes().get("price")+ p.getAttributes().get("purchased");
                    p.getAttributes().put("price", next.price);
                    p.getAttributes().put("purchased", "" + next.purchased);
                    toSave.add(p);
                    shake=shake||(!Utils.equals(initialState,p.getAttributes().get("price")+ p.getAttributes().get("purchased")));
                }


            }
            Controller.getInstance().getDao().saveOrUpdate(toSave);
            this.refreshGuideAccessButton(shake);

        } else {
            //throw alert if guide never has been checked
            if (Controller.getInstance().getDao().paymentNeeded(placeElement)&&
                Utils.isEmpty(placeElement.getAttributes().get("price")))
            {
                 Toast.makeText(this.getActivity(), this.getActivity().getString(R.string.mainupdateprompt), Toast.LENGTH_LONG).show();
            }

        }

    }


}
