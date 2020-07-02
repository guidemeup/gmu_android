package org.gmu.adapters;

import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.gmu.fragments.placedetails.AbstractPlaceDetailFragment;
import org.gmu.fragments.placedetails.ParentPlaceDetailFragment;
import org.gmu.pojo.PlaceElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ahe
 * Date: 27/11/12
 * Time: 10:22
 * To change this template use File | Settings | File Templates.
 */
public class DetailPagerAdapter extends FragmentStatePagerAdapter {
    private List<PlaceElement> elements = new ArrayList();
    private Class type = ParentPlaceDetailFragment.class;

    public DetailPagerAdapter(FragmentManager fm) {
        super(fm);
        clear();

    }

    public void setType(Class type) {
        this.type = type;
    }

    public void clear() {
        this.elements = new ArrayList();
    }

    public void addPlaceElement(PlaceElement elem) {
        this.elements.add(elem);
    }

    public PlaceElement getPlaceElementAt(int position) {
        return this.elements.get(position);
    }

    public CharSequence getPageTitle(int position) {

        return elements.get(position).getTitle();
    }

    @Override
    public int getCount() {
        return elements.size();
    }


    @Override
    public AbstractPlaceDetailFragment getItem(int position) {

        String uid = elements.get(position).getUid();

        AbstractPlaceDetailFragment ret = null;
        try {
            ret = (AbstractPlaceDetailFragment) type.newInstance();
            ret.setUID(uid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;

    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


}