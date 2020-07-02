package org.gmu.ui;
import org.gmu.utils.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


import android.text.Editable;

import android.text.Html.TagHandler;
import android.text.SpannableStringBuilder;

import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ListTagHandler
    implements TagHandler {



    boolean first = true;
    String parent = null;
    int index = 1;
    boolean mustTrim=true;
    private static final char IMG_CHAR=65532;

    private MyProxyContentHandler proxyHandler=null;

    public Map getImgAttribsByUrl(String url)
    {
        List<Map> l= proxyHandler.getImageAttribs();
        int i=-1;
        boolean found=false;
        for ( i = 0; i < l.size(); i++) {
            Map o = l.get(i);

            if(o.get("src")!=null&&o.get("src").equals(url))
            {   found=true;
                break;
            }
        }

        if(found)
        {
            Map ret=l.get(i);
            l.remove(i);
            return ret;
        }
        return null;
    }

    public void handleTag(final boolean opening, final String tag,
                          final Editable output, final XMLReader xmlReader) {

        if(proxyHandler==null) {
            //proxy handler
            proxyHandler = new MyProxyContentHandler(xmlReader.getContentHandler());
            xmlReader.setContentHandler(proxyHandler);
        }

        boolean changed=false;

        if(mustTrim)
        {


            int start=0;
            //trim line breaks at start (remove blogs spaces on feed start)
            if  ((output.length() > 1)&&(output.charAt(0)==IMG_CHAR))
            {   //also remove spaces after first img (normally is place image and doesn't show)
                start=1;

            }
            while ((output.length() > start)&& isSpace(output.charAt(start)))
            {

                output.delete(0,start+1);
                changed=true;
                //mustTrim=false;

            }



        }


        if (tag.equals("ul")) {
            parent = "ul";
            index = 1;
        } else if (tag.equals("ol")) {
            parent = "ol";
            index = 1;
        }
        if (tag.equals("li"))
        {
            char lastChar = 0;
            if (output.length() > 0) {
                lastChar = output.charAt(output.length() - 1);
            }
            if (parent.equals("ul")) {
                if (first) {
                    if (lastChar == '\n') {
                        output.append("\t•  ");
                    } else {
                        output.append("\n\t•  ");
                    }
                    first = false;
                    changed=true;
                } else {
                    //remove empty spaces after ul
                    spaceRemove(output);
                    first = true;
                    changed=true;
                }
            } else {
                if (first) {
                    if (lastChar == '\n') {
                        output.append("\t" + index + ". ");
                    } else {
                        output.append("\n\t" + index + ". ");
                    }
                    first = false;
                    index++;
                    changed=true;
                } else {
                    first = true;
                }
            }
        }

    }

    private static void spaceRemove(Editable output)
    {
        int st=0;
        int end=-1;
        for(int i=output.length()-1;i>0;i--)
        {
            if(output.charAt(i-1)=='*')
            {  if(st<end)
                {

                    if(st==i+1&&end==output.length()-1)
                    {   //empty ul-->remove
                        output.delete(i-1,end);

                    }else
                    {
                        output.delete(st,end);
                    }
                }
               return;
            }
            if(isSpace(output.charAt(i)))
            {

                if(end==-1||st!=i+1)
                {   end=i+1;

                }
                st=i;
            }
        }


    }

    public static boolean isSpace(char t)
    {
        return t=='\n'||t==' '||t=='\t'||t==160;

    }


    private class MyProxyContentHandler implements ContentHandler
    {   private  final String[] STOREDATTRIBS=new String[]{"src","width","height"};
        private ContentHandler delegate;
        private List<Map> imageAttribs=new LinkedList();
        public MyProxyContentHandler(ContentHandler _delegate)
        {
            delegate=_delegate;
        }
        public void setDocumentLocator(Locator locator)
        {
            delegate.setDocumentLocator(locator);
        }

        public void startDocument() throws SAXException {
            delegate.startDocument();
        }

        public void endDocument() throws SAXException {
            delegate.endDocument();
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            delegate.startPrefixMapping(prefix,uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException
        {
            delegate.endPrefixMapping(prefix);
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
        {


            if (localName.equalsIgnoreCase("img"))
            {
                //store image attributes
                HashMap m=new HashMap<String,String>();
                for (int i = 0; i < STOREDATTRIBS.length; i++) {
                    String s = STOREDATTRIBS[i];
                    String value=atts.getValue("",s);
                    if(!Utils.isEmpty(value))
                    {
                        m.put(s,value);
                    }
                }
                imageAttribs.add(m);
            }

            delegate.startElement(uri, localName, qName, atts);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            delegate.endElement(uri, localName, qName);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            delegate.characters( ch,  start,  length);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            delegate.ignorableWhitespace(ch, start, length);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            delegate.processingInstruction(target, data);
        }

        public void skippedEntity(String name) throws SAXException {
            delegate.skippedEntity(name);
        }

        public List<Map> getImageAttribs() {
            return imageAttribs;
        }
    }



}
