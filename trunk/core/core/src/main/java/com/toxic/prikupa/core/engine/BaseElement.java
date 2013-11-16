/**
 * 
 */
package com.toxic.prikupa.core.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import playn.core.CanvasImage;
import playn.core.GroupLayer;
import playn.core.Image;
import playn.core.ImageLayer;
import playn.core.Layer;
import playn.core.PlayN;
import playn.core.Pointer.Event;
import playn.core.TextLayout;
import playn.core.util.Callback;
import pythagoras.f.Point;
import tripleplay.anim.Animation;
import tripleplay.anim.Animator;
import tripleplay.util.Interpolator;
import tripleplay.util.Timer.Handle;

import com.toxic.prikupa.core.engine.handlers.CancelHandler;
import com.toxic.prikupa.core.engine.handlers.HoldHandler;
import com.toxic.prikupa.core.engine.handlers.MoveHandler;
import com.toxic.prikupa.core.engine.handlers.SelectHandler;
import com.toxic.prikupa.core.engine.util.Logger;
import com.toxic.prikupa.core.engine.util.LoggerFactory;
import com.toxic.prikupa.core.engine.util.TimerUtility;

/**
 * @author Strelock
 * 
 */
public class BaseElement {
  
  final static Logger log = LoggerFactory.getLogger(BaseElement.class.getName());

  // ANTS_TAG : create smart caching of reusable cached images - why we should
  // it create again!
  // ANTS_TAG : provide smart hashCode() for wrapped Image object, to
  // determine if it is already existed
  // ANTS_TAG : maybe bound it with Resource manager and CachedImage object.
  // ANTS_TAG : enhanced animation model : add possibility launch animation with
  // Delay or even for signal
  // ANTS_TAG : instance of DRAWABLE_CANVAS : should be wrapped as separate object
  // with additional functionally
  // ANTS_TAG : create smart fast hashCode for snapshot's produced objects, shouldn't 
  // create object, that keeping in memory, and has been already create...
  // ANTS_TAG : provide smart auto-caching engine
  // ANTS_TAG : investigate Workers performance benefits.

  private static final float LINE_WIDTH = 5.0f;

  static CanvasImage DRAWABLE_CANVAS = PlayN.graphics().createImage(1f, 1f);

  protected final static Animator anim = new Animator();

  GroupLayer layer;

  private String id = "NONE";

  private ImageLayer backGround;

  private final Point size = new Point(1f, 1f);

  private boolean propogative = false;

  Backgound bkground;

  private BaseElement parent;

  protected final List<BaseElement> children = new LinkedList<BaseElement>();

  private final Callback<Image> callback = new Callback<Image>() {

    @Override
    public void onSuccess(Image result) {
      renderer();
    }

    @Override
    public void onFailure(Throwable cause) {
      log.error("Cann't load image.\n Reason : \n" + BaseElement.this.bkground.getImage().toString());
    }

  };

  private SelectHandler selectHandler;

  private boolean debug = false;

  private boolean canvasInit = false;

  private TextFormat textFormat;

  private String text;

  private playn.core.TextFormat textFormatInner;

  private MoveHandler moveHandler;

  Event previousEvent;
  Event currentEvent;

  HoldHandler holdHandler;

  Handle holdCancel;

  Map<AnimationType, CancelHandler> animStoppers = new HashMap<AnimationType, CancelHandler>();

  Event initialEvent;

  public BaseElement() {
    this.layer = PlayN.graphics().createGroupLayer();
  }

  private final void initImageLayer() {
    if (this.backGround == null) {
      this.backGround = PlayN.graphics().createImageLayer();
      this.backGround.setWidth(this.size.x * this.layer.scaleX());
      this.backGround.setHeight(this.size.y * this.layer.scaleY());
      this.backGround.setDepth(Integer.MIN_VALUE);
      this.layer.add(this.backGround);
    }
  }

  // ANTS_TAG : enhance this method -> change creating new CanvasObject to
  // resize it parameters.
  private void initTempSizeCanvas() {
    if (this.canvasInit == true) {
      return;
    }
    float elemWidth = this.size.x * this.layer.scaleX();
    float elemHeight = this.size.y * this.layer.scaleY();
    if (!isClipped()
      && this.bkground != null
      && !this.bkground.isResize()
      && this.bkground.getImage() != null
      && this.bkground.getImage().getImage().isReady()
      && (DRAWABLE_CANVAS.width() != this.bkground.getImage().getImage().width() || DRAWABLE_CANVAS.height() != this.bkground
        .getImage().getImage().height())) {
      DRAWABLE_CANVAS = PlayN.graphics().createImage(this.bkground.getImage().getImage().width(),
        this.bkground.getImage().getImage().height());
      this.canvasInit = true;
    }
    else if (DRAWABLE_CANVAS.width() != elemWidth || DRAWABLE_CANVAS.height() != elemHeight) {
      DRAWABLE_CANVAS = PlayN.graphics().createImage(elemWidth, elemHeight);
      this.canvasInit = true;
    }
    else {
      if (!this.canvasInit) {
        DRAWABLE_CANVAS.canvas().clear();
        this.canvasInit = true;
      }
    }
  }

  void renderer() {
    this.canvasInit = false;
    float elemWidth = this.size.x * this.layer.scaleX();
    float elemHeight = this.size.y * this.layer.scaleY();
    if (this.bkground != null) {
      if (this.bkground.getColor() != 0) {
        initTempSizeCanvas();
        DRAWABLE_CANVAS.canvas().setFillColor(this.bkground.getColor());
        DRAWABLE_CANVAS.canvas().fillRect(0, 0, elemWidth, elemHeight);
      }
      if (this.bkground.getImage() != null && this.bkground.getImage().getImage().isReady()) {
        initTempSizeCanvas();
        if (this.bkground.isResize()) {
          DRAWABLE_CANVAS.canvas().drawImage(this.bkground.getImage().getImage(), 0, 0, elemWidth, elemHeight);
        }
        else {
          this.bkground.getImage().getImage().setRepeat(this.bkground.isRepeatX(), this.bkground.isRepeatY());
          if (this.bkground.getImage().getImage().width() < elemWidth
            || this.bkground.getImage().getImage().height() < elemHeight) {
            DRAWABLE_CANVAS.canvas().drawImage(this.bkground.getImage().getImage(), 0, 0,
              this.bkground.getImage().getImage().width(), this.bkground.getImage().getImage().height());
          }
          else {
            if (isClipped()) {
              DRAWABLE_CANVAS.canvas().drawImage(this.bkground.getImage().getImage(), 0, 0, elemWidth, elemHeight, 0,
                0, this.bkground.getImage().getImage().width(), this.bkground.getImage().getImage().height());
            }
            else {
              DRAWABLE_CANVAS.canvas().drawImage(this.bkground.getImage().getImage(), 0, 0, DRAWABLE_CANVAS.width(),
                DRAWABLE_CANVAS.height());
            }
          }
        }
      }
    }
    if (this.text != null && !this.text.isEmpty() && this.textFormat != null) {
      initTempSizeCanvas();
      DRAWABLE_CANVAS.canvas().setFillColor(this.textFormat.getColor());
      if (!this.textFormat.isMargin()) {
        if (this.textFormatInner != null) {

          TextLayout lauoyt = PlayN.graphics().layoutText(this.text, this.textFormatInner);

          switch (this.textFormatInner.align) {
            case LEFT:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, 0f, (elemHeight - lauoyt.height()) / 2f);
              break;

            case CENTER:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, (elemWidth - lauoyt.width()) / 2f,
                (elemHeight - lauoyt.height()) / 2f);
              break;

            case RIGHT:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, (elemWidth - lauoyt.width()),
                (elemHeight - lauoyt.height()) / 2f);
              break;

            default:
              log.error("Something going wrong, you shouldn't see this message!");
              throw new IllegalStateException("Something going wrong, you shouldn't see this message!");
          }

        }
        else {
          TextLayout lauoyt = PlayN.graphics().layoutText(this.text, this.textFormat.getFormat());

          switch (this.textFormatInner.align) {
            case LEFT:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, 0f, (elemHeight - lauoyt.height()) / 2f);
              break;

            case CENTER:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, (elemWidth - lauoyt.width()) / 2f,
                (elemHeight - lauoyt.height()) / 2f);
              break;

            case RIGHT:
              DRAWABLE_CANVAS.canvas().fillText(lauoyt, (elemWidth - lauoyt.width()),
                (elemHeight - lauoyt.height()) / 2f);
              break;

            default:
              PlayN.log().error("Something going wrong, you shouldn't see this message!");
              throw new IllegalStateException("Something going wrong, you shouldn't see this message!");
          }
        }
      }
      else {
        if (this.textFormatInner != null) {

          TextLayout lauoyt = PlayN.graphics().layoutText(this.text, this.textFormatInner);
          DRAWABLE_CANVAS.canvas().fillText(lauoyt, this.textFormat.getMarginLeft(), this.textFormat.getMarginTop());

        }
        else {
          TextLayout lauoyt = PlayN.graphics().layoutText(this.text, this.textFormat.getFormat());
          DRAWABLE_CANVAS.canvas().fillText(lauoyt, this.textFormat.getMarginLeft(), this.textFormat.getMarginTop());
        }
      }
    }
    if (this.debug) {
      initTempSizeCanvas();
      int col = 0;
      if (this.bkground != null) {
        col = this.bkground.getColor();
      }
      DRAWABLE_CANVAS.canvas().setFillColor((col ^ 0xFFFFFFFF) & 0xFF000000);
      DRAWABLE_CANVAS.canvas().setStrokeWidth(LINE_WIDTH);
      DRAWABLE_CANVAS.canvas().drawLine(LINE_WIDTH, LINE_WIDTH, LINE_WIDTH,
        BaseElement.DRAWABLE_CANVAS.height() - 1 - LINE_WIDTH);
      DRAWABLE_CANVAS.canvas().drawLine(LINE_WIDTH, BaseElement.DRAWABLE_CANVAS.height() - 1 - LINE_WIDTH,
        BaseElement.DRAWABLE_CANVAS.width() - 1 - LINE_WIDTH, BaseElement.DRAWABLE_CANVAS.height() - 1 - LINE_WIDTH);
      DRAWABLE_CANVAS.canvas().drawLine(BaseElement.DRAWABLE_CANVAS.width() - 1 - LINE_WIDTH,
        BaseElement.DRAWABLE_CANVAS.height() - 1 - LINE_WIDTH, BaseElement.DRAWABLE_CANVAS.width() - 1 - LINE_WIDTH,
        LINE_WIDTH);
      DRAWABLE_CANVAS.canvas().drawLine(BaseElement.DRAWABLE_CANVAS.width() - 1 - LINE_WIDTH, LINE_WIDTH, LINE_WIDTH,
        LINE_WIDTH);
    }
    initImageLayer();
    if (this.canvasInit) {
      this.backGround.setImage(DRAWABLE_CANVAS.snapshot());
    }
  }

  public void setBackGround(Backgound bkgroundIn) {
    if (this.bkground != null && this.bkground.getImage() != null) {
      this.bkground.getImage().releaseImage();
    }
    this.bkground = bkgroundIn;
    if (this.bkground != null) {
      if (this.bkground.getImage() != null) {
        if (this.bkground.getImage().getImage().isReady()) {
          renderer();
        }
        else {
          this.bkground.getImage().getImage().addCallback(this.callback);
        }
      }
      else {
        if (this.bkground.getColor() != 0) {
          renderer();
        }
      }
    }
  }

  public void addSelectHandler(SelectHandler handly) {
    if (this.selectHandler != null) {
      removeSelectHandler();
    }
    this.selectHandler = handly;
    EventManager.getInstanse().registareTarget(this);
  }

  public void addHoldHandler(HoldHandler handly) {
    if (this.holdHandler != null) {
      removeHoldHandler();
    }
    this.holdHandler = handly;
    EventManager.getInstanse().registareTarget(this);
  }

  public void addMoveHandler(MoveHandler handly) {
    if (this.moveHandler != null) {
      removeMoveHandler();
    }
    this.moveHandler = handly;
    EventManager.getInstanse().registareTarget(this);
  }

  public void removeHandlers() {
    EventManager.getInstanse().unregistareTarget(this);
    this.selectHandler = null;
    this.moveHandler = null;
    this.holdHandler = null;
  }

  public void removeSelectHandler() {
    this.selectHandler = null;
    if (this.moveHandler == null && this.holdHandler == null) {
      EventManager.getInstanse().unregistareTarget(this);
    }
  }

  public void removeHoldHandler() {
    this.holdHandler = null;
    if (this.moveHandler == null && this.selectHandler == null) {
      EventManager.getInstanse().unregistareTarget(this);
    }
  }

  /**
   * <p>
   * Determines weather current Element has at least one assign Handler
   * </p>
   * <br/>
   * 
   * @return true if Element has at least one handler
   */
  public boolean hasHandlers() {
    return !(this.selectHandler == null && this.moveHandler == null && this.holdHandler == null);
  }

  /**
   * <p>
   * Determines weather current Element has assigned {@link HoldHandler}
   * </p>
   * <br/>
   * 
   * @return true if Element has assigned {@link HoldHandler}
   */
  public boolean hasHoldHandlers() {
    return this.holdHandler != null;
  }

  /**
   * <p>
   * Determines weather current Element has assigned {@link SelectHandler}
   * </p>
   * <br/>
   * 
   * @return true if Element has assigned {@link SelectHandler}
   */
  public boolean hasSelectHandlers() {
    return this.selectHandler != null;
  }

  /**
   * <p>
   * Determines weather current Element has assigned {@link MoveHandler}
   * </p>
   * <br/>
   * 
   * @return true if Element has assigned {@link MoveHandler}
   */
  public boolean hasMoveHandlers() {
    return this.moveHandler != null;
  }

  public void removeMoveHandler() {
    if (this.moveHandler == null) {
      EventManager.getInstanse().unregistareTarget(this);
    }
    this.moveHandler = null;
  }

  void disableElements() {
    if (this.selectHandler != null || this.moveHandler != null) {
      EventManager.getInstanse().unregistareTarget(this);
    }
    if (this.bkground != null && this.bkground.getImage() != null) {
      this.bkground.getImage().releaseImage();
    }
    for (BaseElement elem : this.children) {
      elem.disableElements();
    }
  }

  void enableElements() {
    renderer();
    if (this.selectHandler != null || this.moveHandler != null) {
      EventManager.getInstanse().registareTarget(this);
    }
    for (BaseElement elem : this.children) {
      elem.enableElements();
    }
  }

  /**
   * <p>
   * Final event.
   * </p>
   * <br/>
   * 
   * @param e
   */
  void dispatchSelectEvent(final Event e) {
    if(this.initialEvent==null){
      return;
    }
    if (this.moveHandler == null
      || this.previousEvent == null
      || (new Point(this.initialEvent.x(), this.initialEvent.y())
        .distance(this.currentEvent.x(), this.currentEvent.y()) < EventManager.RADIUS_HIT)) {
      this.previousEvent = this.currentEvent;
      this.currentEvent = e;
      if (this.selectHandler != null) {
        this.selectHandler.onSelect(e);
      }
    }
    else {
      this.previousEvent = this.currentEvent;
      this.currentEvent = e;
    }
    this.previousEvent = this.currentEvent;
    this.currentEvent = null;
    this.initialEvent=null;
  }

  /**
   * <p>
   * Intermediate events between initial
   * </p>
   * <br/>
   * 
   * @param e
   */
  void dispatchMoveEvent(final Event e) {
    if(this.initialEvent==null){
      return;
    }
    // ANTS_TAG : this bug of uncontrolled out of boundary events
    this.previousEvent = this.currentEvent;
    this.currentEvent = e;
    if (this.moveHandler != null
      && new Point(BaseElement.this.previousEvent.x(), BaseElement.this.previousEvent.y()).distance(e.x(), e.y()) > EventManager.RADIUS_HIT) {
      if (this.holdCancel != null) {
        this.holdCancel.cancel();
        this.holdCancel = null;
      }
      this.moveHandler.onMove(e);
    }
  }

  /**
   * <p>
   * First event.
   * </p>
   * <br/>
   * 
   * @param event
   */
  void dispatchEventStart(final Event event) {
    this.initialEvent = event;
    this.previousEvent = this.currentEvent;
    this.currentEvent = event;
    if (this.holdHandler != null) {
      if (this.holdCancel != null) {
        log.warn("Should stop previous holdHandler, before registrated next");
        this.holdCancel.cancel();
        this.holdCancel = null;
      }
      // ANTS_TAG : this parameter should be passed from holder handler
      this.holdCancel = TimerUtility.getInstance().atThenEvery(EventManager.TIME_TO_NOTIFY_HOLD, 100, new Runnable() {

        @Override
        public void run() {
          if (BaseElement.this.currentEvent != null
            && (new Point(BaseElement.this.currentEvent.x(), BaseElement.this.currentEvent.y()).distance(event.x(),
              event.y()) < EventManager.RADIUS_HIT)) {
            BaseElement.this.holdHandler.onHold(BaseElement.this.currentEvent);
          }
          else {
            BaseElement.this.holdCancel.cancel();
            BaseElement.this.holdCancel = null;
          }
        }
      });
    }
  }

  void dispatchCancelEvent(Event e) {
    this.previousEvent = this.currentEvent;
    this.currentEvent = e;
    if (this.holdCancel != null) {
      this.holdCancel.cancel();
      this.holdCancel = null;
    }
    this.previousEvent = e;
    this.currentEvent = null;
    log.warn("The previous event has been interrapted! Recieve touch cancel event!" + e.toString());
  }

  public void addChild(BaseElement child) {
    if (child == null) {
      log.error("You try add empty element. The operation will be skipped.");
      throw new IllegalArgumentException("You try add empty element. The operation will be skipped.");
    }
    if (this.children.contains(child)) {
      log.error("Element is already child of this element : " + this.toString());
      throw new IllegalArgumentException("Element is already child of this element : " + this.toString());
    }
    if (child.layer.parent() != null) {
      log.warn("The element is alreay has parent!");
      GroupLayer parentTemp = child.layer.parent();
      parentTemp.remove(child.layer);
    }
    child.parent = this;
    this.layer.add(child.layer);
    addOnRightPlace(child);
    child.enableElements();
  }

  private void addOnRightPlace(BaseElement child) {
    int index = 0;

    while (!this.children.isEmpty() && this.children.get(index).depth() > child.depth()) {
      index++;
    }

    this.children.add(index, child);

  }

  public void removeChild(BaseElement child) {
    if (child.layer.parent() != null && child.layer.parent() == this.layer) {
      GroupLayer parentTemp = child.layer.parent();
      parentTemp.remove(child.layer);
      this.children.remove(child);
      child.parent = null;
      child.disableElements();
    }
    else {
      log.warn("The element has another parent!");
    }
  }

  public void removeChildren() {
    List<BaseElement> list = new ArrayList<BaseElement>(this.children.size());
    list.addAll(this.children);
    while (list.get(0) != null) {
      this.removeChild(list.get(0));
      list.remove(0);
    }
  }

  public void removeFromParent() {
    GroupLayer parentTemp = this.layer.parent();
    parentTemp.remove(this.layer);
  }

  public void setId(String string) {
    this.id = string;
  }

  public void setPosition(float x, float y) {
    this.layer.setTranslation(x, y);
  }

  public void setOrigin(float x, float y) {
    this.layer.setOrigin(x, y);
  }

  public void setRotation(float angle) {
    this.layer.setRotation(angle);
  }

  public void setVisible(boolean flag) {
    this.layer.setVisible(flag);
  }

  public void setScale(float x, float y) {
    if (x <= 0.05f || y <= 0.05f) {
      log.warn("You try scale negative values ! X : " + x + " , Y : " + y);
      throw new IllegalArgumentException("You try scale negative values ! X : " + x + " , Y : " + y);
    }

    this.layer.setScale(x, y);
  }

  public void setScale(float scale) {
    if (scale <= 0.05f) {
      log.warn("You try scale negative value ! scale : " + scale);
      throw new IllegalArgumentException("You try scale negative value ! scale : " + scale);
    }
    this.layer.setScale(scale, scale);
  }

  public void scale(float x, float y) {
    if (x <= 0 || y <= 0) {
      throw new IllegalArgumentException("Scale couldn't be have negative values!");
    }
    this.layer.setScale(x, y);
  }

  public Point position() {
    return new Point(this.layer.tx(), this.layer.ty());
  }

  public Point scale() {
    return new Point(this.layer.scaleX(), this.layer.scaleY());
  }

  public void setWidth(float width) {
    if (this.backGround != null) {
      this.backGround.setWidth(width);
    }
    this.size.x = width / this.layer.scaleX();
  }

  public void setHeight(float height) {
    if (this.backGround != null) {
      this.backGround.setHeight(height);
    }
    this.size.y = height / this.layer.scaleY();
  }

  public void setSize(float x, float y) {
    if (x <= 0 || y <= 0) {
      log.warn("Couldn't setting up negative argument!");
      throw new IllegalArgumentException("Couldn't setting up negative argument!");
    }
    if (this.backGround != null) {
      this.backGround.setWidth(x);
      this.backGround.setHeight(y);
    }
    if (this.layer instanceof GroupLayer.Clipped) {
      ((GroupLayer.Clipped) this.layer).setSize(x, y);
    }
    this.size.x = x / this.layer.scaleX();
    this.size.y = y / this.layer.scaleY();
  }

  public void setDepth(float depth) {
    this.layer.setDepth(depth);
  }

  public float depth() {
    return this.layer.depth();
  }

  public void setAplha(float alpha) {
    if (alpha >= 0.0f && alpha <= 1.0f) {
      this.layer.setAlpha(alpha);
    }
    else {
      log.warn("Alpha parametr should be between [0.0:1.0]");
      throw new IllegalArgumentException("Alpha parametr should be between [0.0:1.0]");
    }
  }

  public boolean isActive() {
    Layer tempLayer = this.layer;
    while (tempLayer.visible() && tempLayer.parent() != null) {
      tempLayer = tempLayer.parent();
      if (tempLayer == PlayN.graphics().rootLayer()) {
        // PlayN.log().debug("The elements is active : " +
        // this.toString());
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "The element id = " + this.id + ", position :[" + this.layer.tx() + ":" + this.layer.ty() + "] size : ["
      + width() + ":" + height() + "]";
  }

  boolean hitTest(Event e) {
    // PlayN.log().debug("\nEvent :\n" + e.toString() + "\nOf object : " +
    // this.toString());
    Point localAxis = Layer.Util.screenToLayer(this.layer, e.x(), e.y());
    // PlayN.log().debug("Converted coord : [" + localAxis.x + ":" +
    // localAxis.y + "].\n");
    if (localAxis.x() > 0 && localAxis.y() > 0 && width() > localAxis.x() && height() > localAxis.y() && isActive()) {
      // PlayN.log().debug("Event hit object : " + this.toString());
      return true;
    }
    // PlayN.log().debug("Event out of boundares of object : " +
    // this.toString());
    return false;
  }

  public void setDebug(boolean flag) {
    this.debug = flag;
    renderer();
  }

  public final CancelHandler animateShake(final float amplitudeX, final float amplitudeY, final Interpolator mode,
    final float duration) {
    if (mode == null) {
      PlayN.log().error("You have tried animate element : " + this.toString() + "\n" + "with empty Interpolator.");
      throw new IllegalArgumentException("You have tried animate element : " + this.toString() + "\n"
        + "with empty Interpolator.");
    }
    if (duration <= 0) {
      PlayN.log().error(
        "You have set negative time to animation on element : " + this.toString() + "\n with value : " + duration);
      throw new IllegalStateException("You have set negative time to animation on element : " + this.toString()
        + "\n with value : " + duration);
    }
    if (amplitudeX < 0 || amplitudeY < 0) {
      PlayN.log().error(
        "You have set negative amplitudes value to animation on element : " + this.toString() + "\n with value : ["
          + amplitudeX + ":" + amplitudeY + "].");
      throw new IllegalStateException("You have set negative amplitudes value to animation on element : "
        + this.toString() + "\n with value : [" + amplitudeX + ":" + amplitudeY + "].");
    }
    if (this.animStoppers.get(AnimationType.SHAKE) != null) {
      PlayN.log().warn(
        "Previous shake animation with element : " + this.toString()
          + "Doesn't yet finished.\nWill be forced to stop now.");
      this.animStoppers.get(AnimationType.SHAKE).cancel();
    }

    final CancelHandler cancel = new CancelHandler() {

      private final tripleplay.anim.Animation.Handle cancelHandler = anim.shake(BaseElement.this.layer)
        .bounds(-amplitudeX, amplitudeX, -amplitudeY, amplitudeY).using(mode).in(duration).then()
        .action(new Runnable() {

          @Override
          public void run() {
            if (BaseElement.this.animStoppers.get(AnimationType.SHAKE) != null) {
              BaseElement.this.animStoppers.get(AnimationType.SHAKE).cancel();
            }
          }
        }).handle();

      @Override
      public void cancel() {
        log.debug("Stopped previous SHAKE animation!");
        this.cancelHandler.cancel();
        BaseElement.this.animStoppers.remove(AnimationType.SHAKE);
      }

    };
    this.animStoppers.put(AnimationType.SHAKE, cancel);
    return cancel;
  }

  /**
   * <p>
   * Simple animation transition to supplied point
   * </p>
   * <br/>
   * 
   * @param x
   *          location relative to axis X
   * @param y
   *          location relative to axis Y
   * @param mode
   *          the animation behavior mode
   */
  public final CancelHandler animateTransition(final float x, final float y, final Interpolator mode,
    final float duration) {
    if (mode == null) {
      PlayN.log().error("You have tried animate element : " + this.toString() + "\n" + "with empty Interpolator.");
      throw new IllegalArgumentException("You have tried animate element : " + this.toString() + "\n"
        + "with empty Interpolator.");
    }
    if (duration <= 0) {
      PlayN.log().error(
        "You have set negative time to animation on element : " + this.toString() + "\n with value : " + duration);
      throw new IllegalStateException("You have set negative time to animation on element : " + this.toString()
        + "\n with value : " + duration);
    }
    if (this.animStoppers.get(AnimationType.TRANSITION) != null) {
      PlayN.log().warn(
        "Previous shake animation with element : " + this.toString()
          + "Doesn't yet finished.\nWill be forced to stop now.");
      this.animStoppers.get(AnimationType.TRANSITION).cancel();
    }

    final CancelHandler cancel = new CancelHandler() {

      private final tripleplay.anim.Animation.Handle cancelHandler = anim.tweenTranslation(BaseElement.this.layer)
        .in(duration).from(BaseElement.this.layer.tx(), BaseElement.this.layer.ty()).to(x, y).using(mode).then()
        .action(new Runnable() {

          @Override
          public void run() {
            if (BaseElement.this.animStoppers.get(AnimationType.TRANSITION) != null) {
              BaseElement.this.animStoppers.get(AnimationType.TRANSITION).cancel();
            }
          }
        }).handle();

      @Override
      public void cancel() {
        log.debug("Stopped previous TRANSITION animation!");
        if (this.cancelHandler != null) {
          this.cancelHandler.cancel();
        }
        BaseElement.this.animStoppers.remove(AnimationType.TRANSITION);
      }

    };
    this.animStoppers.put(AnimationType.TRANSITION, cancel);
    return cancel;
  }

  public final CancelHandler animateAction(final CustomAnimation action, final Interpolator mode, final int duration) {
    if (mode == null) {
      log.error("You have tried animate element : " + this.toString() + "\n" + "with empty Interpolator.");
      throw new IllegalArgumentException("You have tried animate element : " + this.toString() + "\n"
        + "with empty Interpolator.");
    }
    if (action == null) {
      log.error("You have tried animate empty CustomAction on element : " + this.toString());
      throw new IllegalArgumentException("You have tried animate empty CustomAction on element : " + this.toString());
    }
    if (duration <= 0) {
      log.error(
        "You have set negative time to animation on element : " + this.toString() + "\n with value : " + duration);
      throw new IllegalStateException("You have set negative time to animation on element : " + this.toString()
        + "\n with value : " + duration);
    }
    if (this.animStoppers.get(AnimationType.CUSTOM) != null) {
      log.warn(
        "Previous shake animation with element : " + this.toString()
          + "Doesn't yet finished.\nWill be forced to stop now.");
      this.animStoppers.get(AnimationType.CUSTOM).cancel();
    }

    final CancelHandler cancel = new CancelHandler() {

      private final tripleplay.anim.Animation.Handle cancelHandler = anim.tween(new Animation.Value() {

        @Override
        public void set(float value) {
          action.uppdate(value);
        }

        @Override
        public float initial() {
          return 0;
        }
      }).from(0.0f).to(1.0f).using(mode).in(duration).then().action(new Runnable() {

        @Override
        public void run() {
          if (BaseElement.this.animStoppers.get(AnimationType.CUSTOM) != null) {
            BaseElement.this.animStoppers.get(AnimationType.CUSTOM).cancel();
          }
        }
      }).handle();

      @Override
      public void cancel() {
        log.debug("Stopped previous CUSTOM animation!");
        if (this.cancelHandler != null) {
          this.cancelHandler.cancel();
        }
        BaseElement.this.animStoppers.remove(AnimationType.CUSTOM);
      }

    };
    this.animStoppers.put(AnimationType.CUSTOM, cancel);
    return cancel;
  }

  public final CancelHandler animateRotate(final float angle, final Interpolator mode, final int duration) {
    if (mode == null) {
      PlayN.log().error("You have tried animate element : " + this.toString() + "\n" + "with empty Interpolator.");
      throw new IllegalArgumentException("You have tried animate element : " + this.toString() + "\n"
        + "with empty Interpolator.");
    }
    if (duration <= 0) {
      PlayN.log().error(
        "You have set negative time to animation on element : " + this.toString() + "\n with value : " + duration);
      throw new IllegalStateException("You have set negative time to animation on element : " + this.toString()
        + "\n with value : " + duration);
    }
    if (this.animStoppers.get(AnimationType.ROTATE) != null) {
      PlayN.log().warn(
        "Previous shake animation with element : " + this.toString()
          + "Doesn't yet finished.\nWill be forced to stop now.");
      this.animStoppers.get(AnimationType.ROTATE).cancel();
    }

    final CancelHandler cancel = new CancelHandler() {

      private final tripleplay.anim.Animation.Handle cancelHandler = anim.tweenRotation(BaseElement.this.layer)
        .in(duration).from(BaseElement.this.layer.rotation()).to(BaseElement.this.layer.rotation() + angle).using(mode)
        .then().action(new Runnable() {

          @Override
          public void run() {
            if (BaseElement.this.animStoppers.get(AnimationType.ROTATE) != null) {
              BaseElement.this.animStoppers.get(AnimationType.ROTATE).cancel();
            }
          }
        }).handle();

      @Override
      public void cancel() {
        log.debug("Stopped previous ROTATE animation!");
        if (this.cancelHandler != null) {
          this.cancelHandler.cancel();
        }
        BaseElement.this.animStoppers.remove(AnimationType.ROTATE);
      }

    };
    this.animStoppers.put(AnimationType.ROTATE, cancel);
    return cancel;
  }

  public final CancelHandler animateOpacity(final float to, final Interpolator mode, final int duration) {
    if (mode == null) {
      log.error("You have tried animate element : " + this.toString() + "\n" + "with empty Interpolator.");
      throw new IllegalArgumentException("You have tried animate element : " + this.toString() + "\n"
        + "with empty Interpolator.");
    }
    if (to < 0 || to > 1.0f) {
      log.warn(
        "You have pushed Illigal value for widget : " + this.toString() + "\n"
          + "It can change from [0:1.0] . You pushed : " + to);
      throw new IllegalArgumentException("You have pushed Illigal value for widget : " + this.toString() + "\n"
        + "It can change from [0:1.0] . You pushed : " + to);
    }
    if (duration <= 0) {
      PlayN.log().error(
        "You have set negative time to animation on element : " + this.toString() + "\n with value : " + duration);
      throw new IllegalStateException("You have set negative time to animation on element : " + this.toString()
        + "\n with value : " + duration);
    }
    if (this.animStoppers.get(AnimationType.OPACITY) != null) {
      PlayN.log().warn(
        "Previous shake animation with element : " + this.toString()
          + "Doesn't yet finished.\nWill be forced to stop now.");
      this.animStoppers.get(AnimationType.OPACITY).cancel();
    }

    final CancelHandler cancel = new CancelHandler() {

      private final tripleplay.anim.Animation.Handle cancelHandler = anim.tweenAlpha(BaseElement.this.layer)
        .in(duration).from(BaseElement.this.layer.alpha()).to(to).using(mode).then().action(new Runnable() {

          @Override
          public void run() {
            if (BaseElement.this.animStoppers.get(AnimationType.OPACITY) != null) {
              BaseElement.this.animStoppers.get(AnimationType.OPACITY).cancel();
            }
          }
        }).handle();

      @Override
      public void cancel() {
        log.debug("Stopped previous OPACITY animation!");
        if (this.cancelHandler != null) {
          this.cancelHandler.cancel();
        }
        BaseElement.this.animStoppers.remove(AnimationType.OPACITY);
      }

    };
    this.animStoppers.put(AnimationType.OPACITY, cancel);
    return cancel;
  }

  /**
   * <p>
   * Stop all currently executed animation.
   * </p>
   * <br/>
   */
  public void stopAnimation() {
    for (AnimationType type : this.animStoppers.keySet()) {
      if (this.animStoppers.get(type) != null) {
        this.animStoppers.get(type).cancel();
        this.animStoppers.remove(type);
      }
    }
  }

  public void stopShakeAnimation() {
    if (this.animStoppers.get(AnimationType.SHAKE) != null) {
      this.animStoppers.get(AnimationType.SHAKE).cancel();
      this.animStoppers.remove(AnimationType.SHAKE);
    }
  }

  public void stopTransitionAnimation() {
    if (this.animStoppers.get(AnimationType.TRANSITION) != null) {
      this.animStoppers.get(AnimationType.TRANSITION).cancel();
      this.animStoppers.remove(AnimationType.TRANSITION);
    }
  }

  public void stopActionAnimation() {
    if (this.animStoppers.get(AnimationType.CUSTOM) != null) {
      this.animStoppers.get(AnimationType.CUSTOM).cancel();
      this.animStoppers.remove(AnimationType.CUSTOM);
    }
  }

  public void stopRotateAnimation() {
    if (this.animStoppers.get(AnimationType.ROTATE) != null) {
      this.animStoppers.get(AnimationType.ROTATE).cancel();
      this.animStoppers.remove(AnimationType.ROTATE);
    }
  }

  public void stopOpacityAnimation() {
    if (this.animStoppers.get(AnimationType.OPACITY) != null) {
      this.animStoppers.get(AnimationType.OPACITY).cancel();
      this.animStoppers.remove(AnimationType.OPACITY);
    }
  }

  public boolean isAnimated() {
    for (AnimationType type : this.animStoppers.keySet()) {
      if (this.animStoppers.get(type) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * <p>
   * Used for handy difference of animation
   * {@link tripleplay.anim.Animation.Handle}.
   * </p>
   * <br/>
   * 
   * @author Strelock
   * 
   */
  public enum AnimationType {
    OPACITY, ROTATE, CUSTOM, TRANSITION, SHAKE;
  }

  public boolean isClipped() {
    return (this.layer instanceof GroupLayer.Clipped);
  }

  public void setClipped(boolean flag) {
    if (flag && !(this.layer instanceof GroupLayer.Clipped)) {
      log.info("Create clipped");
      GroupLayer parentTemp = this.layer.parent();
      GroupLayer wrapper = PlayN.graphics().createGroupLayer(width(), height());
      wrapper.setTx(this.layer.tx());
      wrapper.setTy(this.layer.ty());
      wrapper.setOrigin(this.layer.originX(), this.layer.originY());
      this.layer.setTx(0f);
      this.layer.setTy(0f);
      this.layer.setOrigin(0, 0);
      if (parentTemp != null) {
        log.warn("parent doesn't equal null");
        parentTemp.remove(this.layer);
      }
      wrapper.add(this.layer);
      if (parentTemp != null) {
        log.warn("parent doesn't equal null");
        parentTemp.add(wrapper);
      }
      this.layer = wrapper;
      log.warn("created.");
    }
    else {
      GroupLayer parentTemp = this.layer.parent();
      GroupLayer unClipped = (GroupLayer) this.layer.get(0);
      unClipped.setTx(this.layer.tx());
      unClipped.setTy(this.layer.ty());
      unClipped.setOrigin(this.layer.originX(), this.layer.originY());
      if (parentTemp != null) {
        parentTemp.remove(this.layer);
      }
      this.layer.remove(unClipped);
      this.layer = unClipped;
      if (parentTemp != null) {
        parentTemp.add(this.layer);
      }
    }
  }

  public void setTextFromat(TextFormat format) {
    if (format.getFormat().shouldWrap() || format.getFormat().wrapWidth > width()) {
      this.textFormatInner = new playn.core.TextFormat(format.getFormat().font, height(), format.getFormat().align);
    }
    else {
      this.textFormatInner = null;
    }
    this.textFormat = format;
    renderer();
  }

  public float width() {
    return this.size.x * this.layer.scaleX();
  }

  public float height() {
    return this.size.y * this.layer.scaleY();
  }

  public float alpha() {
    return this.layer.alpha();
  }

  public float originX() {
    return this.layer.originX();
  }

  public float originY() {
    return this.layer.originY();
  }

  public float positionX() {
    return this.layer.tx();
  }

  public float positionY() {
    return this.layer.ty();
  }

  public BaseElement getParent() {
    return this.parent;
  }

  public boolean visible() {
    return this.layer.visible();
  }

  public boolean isPropogative() {
    return this.propogative;
  }

  public void setPropogative(boolean propogativeIn) {
    this.propogative = propogativeIn;
  }

  /**
   * @return weather this instance of {@link BaseElement} is Root - Scene
   *         element.
   */
  @SuppressWarnings("static-method")
  public boolean isRoot() {
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BaseElement)) {
      return false;
    }

    BaseElement elem = (BaseElement) obj;

    if (this == elem) {
      return true;
    }

    if (elem.parent == this.parent && elem.size.equals(this.size) && elem.alpha() == this.alpha()
      && elem.propogative == this.propogative && elem.selectHandler == this.selectHandler
      && elem.moveHandler == this.moveHandler && elem.id.equals(this.id) && elem.layer.equals(this.layer)) {
      return true;
    }

    return false;
  }

  float getRealDepth(BaseElement child) {

    if (child == null) {
      log.error("You've try get index of null element, in the object : " + toString());
      return -1;
    }

    return this.children.indexOf(child);
  }

  public void drawText(String value) {
    this.text = value;
    renderer();
  }

}
