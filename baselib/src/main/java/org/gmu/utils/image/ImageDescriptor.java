package org.gmu.utils.image;

/**
 * User: ttg
 * Date: 17/01/13
 * Time: 13:05
 * To change this template use File | Settings | File Templates.
 */
public class ImageDescriptor {
    public String uri;
    public int size;
    public String key;
    public ImageDescriptor(String uri, int size) {
             this(uri,size,false);
    }
    public ImageDescriptor(String uri, int size,boolean scaleUp) {
        this.uri = uri;
        this.size = size;
        this.key = uri + size;
        if(scaleUp) this.key=this.key+ImageLoader.SCALEUPTAG;
    }
}
