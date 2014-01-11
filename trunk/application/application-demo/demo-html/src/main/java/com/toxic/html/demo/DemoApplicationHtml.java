package com.toxic.html.demo;

import com.toxic.core.engine.test.DemoApplication;
import com.toxic.html.ApplicationHtml;


/**
 * <p>
 *  Entry point of 'html' module.
 * </p>
 * <br/>
 * @author Strelock
 *
 */
public class DemoApplicationHtml extends ApplicationHtml {

  @Override
  public void setApplication() {
    this.application = new DemoApplication();
  }


}
