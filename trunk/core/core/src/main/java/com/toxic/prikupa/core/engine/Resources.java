/**
 * 
 */
package com.toxic.prikupa.core.engine;

import java.util.HashMap;
import java.util.Map;

import playn.core.Image;
import playn.core.Platform.Type;
import playn.core.PlayN;
import playn.core.Sound;
import playn.core.util.Callback;

/**
 * @author Strelock
 * 
 */
public class Resources {

  static int callbacks_counter = 0;

  public static boolean isLoaded() {
    return callbacks_counter == 0;
  }

  static class ImageResource {

    public ImageResource(Image im) {
      this.image = im;
      this.counter = 1;
    }

    final Image image;
    int counter;

    void increase() {
      this.counter++;
    }

    void decrease() {
      this.counter--;
    }

    int count() {
      return this.counter;
    }

  }

  static class AudioResource {

    private final Sound audioVal;
    int counter;

    public AudioResource(Sound sound) {
      this.audioVal = sound;
      this.counter = 1;
    }

    void increase() {
      this.counter++;
    }

    void decrease() {
      this.counter--;
    }

    int count() {
      return this.counter;
    }

    public Sound getAudio() {
      return this.audioVal;
    }

  }

  static class TextResource {
    public TextResource(String val) {
      this.textVal = val;
      this.counter = 1;
    }

    private final String textVal;
    int counter;

    void increase() {
      this.counter++;
    }

    void decrease() {
      this.counter--;
    }

    int count() {
      return this.counter;
    }

    public String getText() {
      return this.textVal;
    }
  }

  static final Map<String, ImageResource> images = new HashMap<String, ImageResource>();
  private static final Map<String, AudioResource> audio = new HashMap<String, AudioResource>();
  static final Map<String, TextResource> text = new HashMap<String, TextResource>();

  static class ImageCallback implements Callback<Image> {

    private final String path;

    public ImageCallback(String s) {
      this.path = s;
    }

    @Override
    public final void onSuccess(Image result) {
      if (images.containsKey(this.path)) {
        PlayN.log().warn(
          "Something going wrong you have load twice already existing resource!" + "\n Path : " + this.path);
      }
      else {
        images.put(this.path, new ImageResource(result));
      }
      callbacks_counter--;
    }

    @Override
    public final void onFailure(Throwable cause) {
      PlayN.log().warn("Couldn't find element with path : " + this.path);
    }

  }

  static class TextCallback implements Callback<String> {

    private final String path;
    private final Callback<String> callback;

    public TextCallback(String s, Callback<String> callback) {
      this.path = s;
      this.callback = callback;
    }

    @Override
    public final void onSuccess(String result) {
      if (text.containsKey(this.path)) {
        PlayN.log().warn(
          "Something going wrong you have load twice already existing resource!" + "\n Path : " + this.path);
        text.get(this.path).increase();
      }
      else {
        text.put(this.path, new TextResource(result));
        this.callback.onSuccess(result);
      }
      callbacks_counter--;
    }

    @Override
    public final void onFailure(Throwable cause) {
      PlayN.log().warn("Couldn't find element with path : " + this.path);
      this.callback.onFailure(cause);
    }

  }

  public static Image getImage(String path) {
    if (images.get(path) == null) {
      if (PlayN.platformType() == Type.HTML || PlayN.platformType() == Type.FLASH) {
        images.put(path, new ImageResource(PlayN.assets().getImage(path)));
        images.get(path).image.addCallback(new ImageCallback(path));
        callbacks_counter++;
      }
      else {
        images.put(path, new ImageResource(PlayN.assets().getImageSync(path)));
      }
    }
    else {
      images.get(path).increase();
    }
    return images.get(path).image;
  }

  public static Sound getSound(String path) {
    if (audio.get(path) == null) {
      audio.put(path, new AudioResource(PlayN.assets().getSound(path)));
    }
    else {
      audio.get(path).increase();
    }
    return audio.get(path).getAudio();
  }

  public static String getText(String path, final Callback<String> callback) {
    if (text.get(path) == null) {
      PlayN.assets().getText(path, new TextCallback(path, callback));
      callbacks_counter++;
    }
    else {
      text.get(path).increase();
    }
    return text.get(path).getText();
  }

  public static void release(String path) {
    if (images.get(path) != null) {
      images.get(path).decrease();
      if (images.get(path).count() <= 0) {
        images.remove(path);
      }
    }
  }

}
