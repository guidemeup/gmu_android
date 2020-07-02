package org.gmu.activities.alpha.pro;


import org.gmu.config.ConfigBean;
import org.gmu.config.Constants;

/**
 * User: ttg
 * Date: 26/03/13
 * Time: 10:51
 * To change this template use File | Settings | File Templates.
 */
public class Config extends ConfigBean {


    public Config()
    {
        super();
        setPackageName("org.gmu.activities.alpha.pro");
        setRootId(Constants.GUIDEMEUP_ROOTID);
        setMainGuideId(null);
        setRootName("GuideMeUp");
        setBaseServer("http://www.guidemeup.com");
        setBaseServerGuides(getBaseServer()+"/guides");
        setAppType(APP_TYPE.STORE);
       // setPurchaseManager(new GooglePurchaseManager("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmPJG6l5Qvf7bbsQVSBp8htTpT+3TgQkY4QSOEqyYx7K5UvbQI93K2Adx7nT9eJqNRgVPatUij2Uy2wS3Yh+7MCnaIAET7LAJrty6wxKviSY69T+j8gnAkAaF+0V9iCt6mo4r7IpWGJuATuEBhg5s2j4I1iaDSwEzWG2vebTNlk4HcXsjlJYEXKaqgqfrYM2wsRtobpFfjqO91utnvg93Z08DhNefoIOpEadCEUumiIFRzhN7A4cpCYcqvLqTKIS3mlO91+je5AySN9QZripT+KODVjjH/VGAi1bnlA4AsHnTm9muTXbWmDmOHKdNzZmy/+Dv4W/Mttp1xTp5YuhO5QIDAQAB"));
       // setTheme(R.style.GMU_Theme_Sherlock_Light_DarkActionBar);
      //  setAttribute(IConfig.TYPE_ORDER_PRIOR,IConfig.TYPE_ORDER_PRIOR_SAME_PRIOR);

        //setAttributeArray(IConfig.ORDER_DEFINITION,new  String[] {OrderDefinition.PRIOR_RATTING,
         //       OrderDefinition.PRIOR_DISTANCE,OrderDefinition. PRIOR_PREDEFINED,OrderDefinition.PRIOR_TITLE});

    }



}
