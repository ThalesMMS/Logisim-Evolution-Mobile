/* SPDX-License-Identifier: GPL-3.0-only */
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.data.Attribute;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedCharacterIterator;
import java.util.*;
import java.util.List;

/**
 * Records what the desktop TTL factories draw with ShowInternalStructure=true.
 *
 * <p>For every model, the oracle creates the factory with its default attributes (facing east, location 0,0),
 * enables DRAW_INTERNAL_STRUCTURE and calls the private AbstractTtlGate.paintInternalBase, which paintInstance
 * invokes after drawPorts and drawLabel. A recording Graphics2D forwards every call to a BufferedImage, so the
 * PNG is the desktop Java2D raster, and writes the same calls as a display list in component coordinates.
 * There is no circuit state. Models that create their register data while painting draw its state dots; lines
 * painted by drawState, structureDot or drawBit are marked with a leading "~" and are not part of the structure.
 * Lines from Ttl74123.drawTiming, whose text depends on the 1Rext/1Cext/2Rext/2Cext attributes, start with "^".
 *
 * <p>Usage: java -Djava.awt.headless=true TtlInternalStructureOracle models.txt out-dir variant [scale]
 * where variant is "shaped" (ANSI, also used for DIN 40700) or "rectangular" (IEC).
 */
public final class TtlInternalStructureOracle {
  static final List<String> lines = new ArrayList<>();
  /** Helpers that paint the stored register value; their output depends on simulation state. */
  static final Set<String> STATE_PAINTERS = Set.of("drawState", "structureDot", "drawBit");

  /** Ttl74123.drawTiming prints the pulse width computed from the Rext and Cext attributes. */
  static final Set<String> ATTRIBUTE_PAINTERS = Set.of("drawTiming");

  static void emit(String line) {
    for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
      if (!frame.getClassName().startsWith("com.cburch.logisim.std.ttl.")) continue;
      if (STATE_PAINTERS.contains(frame.getMethodName())) {
        lines.add("~" + line);
        return;
      }
      if (ATTRIBUTE_PAINTERS.contains(frame.getMethodName())) {
        lines.add("^" + line);
        return;
      }
    }
    lines.add(line);
  }
  static final Set<String> warnings = new TreeSet<>();

  static String n(double v) {
    double r = Math.rint(v * 1000) / 1000;
    if (r == Math.rint(r)) return Long.toString((long) r);
    String s = String.format(Locale.ROOT, "%.3f", r);
    while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
    return s;
  }

  static String color(Color c) {
    return String.format(Locale.ROOT, "%08x", c.getRGB());
  }

  static final class Recorder extends Graphics2D {
    final Graphics2D delegate;
    final AffineTransform base;
    AffineTransform user;
    Color color = Color.BLACK;
    Stroke stroke = new BasicStroke(1f);
    Font font;

    Recorder(Graphics2D delegate, AffineTransform base, AffineTransform user, Font font) {
      this.delegate = delegate;
      this.base = base;
      this.user = new AffineTransform(user);
      this.font = font;
      delegate.setColor(color);
      delegate.setStroke(stroke);
      delegate.setFont(font);
      applyTransform();
    }

    void applyTransform() {
      AffineTransform t = new AffineTransform(base);
      t.concatenate(user);
      delegate.setTransform(t);
    }

    double width() {
      return stroke instanceof BasicStroke b ? b.getLineWidth() : 1.0;
    }

    boolean translationOnly() {
      int type = user.getType();
      return (type & ~AffineTransform.TYPE_TRANSLATION) == 0;
    }

    void recordPath(Shape shape, boolean fill) {
      PathIterator it = shape.getPathIterator(user);
      StringBuilder b = new StringBuilder();
      double[] c = new double[6];
      while (!it.isDone()) {
        int type = it.currentSegment(c);
        switch (type) {
          case PathIterator.SEG_MOVETO -> b.append(" M ").append(n(c[0])).append(' ').append(n(c[1]));
          case PathIterator.SEG_LINETO -> b.append(" L ").append(n(c[0])).append(' ').append(n(c[1]));
          case PathIterator.SEG_QUADTO -> b.append(" Q ").append(n(c[0])).append(' ').append(n(c[1])).append(' ')
              .append(n(c[2])).append(' ').append(n(c[3]));
          case PathIterator.SEG_CUBICTO -> b.append(" C ").append(n(c[0])).append(' ').append(n(c[1])).append(' ')
              .append(n(c[2])).append(' ').append(n(c[3])).append(' ').append(n(c[4])).append(' ').append(n(c[5]));
          case PathIterator.SEG_CLOSE -> b.append(" Z");
          default -> { }
        }
        it.next();
      }
      if (b.length() == 0) return;
      emit("P " + (fill ? "f" : "s") + " " + color(color) + " " + n(width()) + b);
    }

    void recordBox(char kind, double x, double y, double w, double h, double radius, boolean fill) {
      if (!translationOnly()) {
        Shape shape = kind == 'O' ? new Ellipse2D.Double(x, y, w, h)
            : radius > 0 ? new RoundRectangle2D.Double(x, y, w, h, radius * 2, radius * 2) : new Rectangle2D.Double(x, y, w, h);
        recordPath(shape, fill);
        return;
      }
      double tx = user.getTranslateX(), ty = user.getTranslateY();
      emit(kind + " " + (fill ? "f" : "s") + " " + color(color) + " " + n(width()) + " " + n(x + tx) + " " + n(y + ty)
          + " " + n(w) + " " + n(h) + (kind == 'R' ? " " + n(radius) : ""));
    }

    void recordText(String text, double x, double y) {
      if (text.isEmpty()) return;
      FontMetrics metrics = delegate.getFontMetrics(font);
      double advance = metrics.stringWidth(text);
      Point2D anchor = user.transform(new Point2D.Double(x + advance / 2, y), null);
      double rotation = Math.toDegrees(Math.atan2(user.getShearY(), user.getScaleX()));
      String family = font.getFamily(Locale.ROOT);
      String style = (font.isBold() ? "b" : "") + (font.isItalic() ? "i" : "");
      emit("T " + color(color) + " " + family.replace(' ', '_') + " " + n(font.getSize2D()) + " " + (style.isEmpty() ? "n" : style)
          + " " + n(anchor.getX()) + " " + n(anchor.getY()) + " " + n(rotation) + " " + n(advance) + " " + text);
    }

    // Graphics state
    @Override public Graphics create() {
      Recorder copy = new Recorder((Graphics2D) delegate.create(), base, user, font);
      copy.setColor(color);
      copy.setStroke(stroke);
      return copy;
    }
    @Override public Color getColor() { return color; }
    @Override public void setColor(Color c) { if (c != null) { color = c; delegate.setColor(c); } }
    @Override public void setPaint(Paint paint) { if (paint instanceof Color c) setColor(c); else { warnings.add("non-colour paint"); delegate.setPaint(paint); } }
    @Override public Paint getPaint() { return color; }
    @Override public void setPaintMode() { delegate.setPaintMode(); }
    @Override public void setXORMode(Color c) { warnings.add("XOR mode"); delegate.setXORMode(c); }
    @Override public Font getFont() { return font; }
    @Override public void setFont(Font f) { if (f != null) { font = f; delegate.setFont(f); } }
    @Override public FontMetrics getFontMetrics(Font f) { return delegate.getFontMetrics(f); }
    @Override public Stroke getStroke() { return stroke; }
    @Override public void setStroke(Stroke s) { stroke = s; delegate.setStroke(s); }
    @Override public Rectangle getClipBounds() { return delegate.getClipBounds(); }
    @Override public void clipRect(int x, int y, int w, int h) { warnings.add("clip"); delegate.clipRect(x, y, w, h); }
    @Override public void setClip(int x, int y, int w, int h) { delegate.setClip(x, y, w, h); }
    @Override public Shape getClip() { return delegate.getClip(); }
    @Override public void setClip(Shape clip) { delegate.setClip(clip); }
    @Override public void clip(Shape s) { warnings.add("clip"); delegate.clip(s); }
    @Override public void dispose() { delegate.dispose(); }
    @Override public Composite getComposite() { return delegate.getComposite(); }
    @Override public void setComposite(Composite comp) { delegate.setComposite(comp); }
    @Override public void setRenderingHint(RenderingHints.Key k, Object v) { delegate.setRenderingHint(k, v); }
    @Override public Object getRenderingHint(RenderingHints.Key k) { return delegate.getRenderingHint(k); }
    @Override public void setRenderingHints(Map<?, ?> hints) { delegate.setRenderingHints(hints); }
    @Override public void addRenderingHints(Map<?, ?> hints) { delegate.addRenderingHints(hints); }
    @Override public RenderingHints getRenderingHints() { return delegate.getRenderingHints(); }
    @Override public void setBackground(Color c) { delegate.setBackground(c); }
    @Override public Color getBackground() { return delegate.getBackground(); }
    @Override public FontRenderContext getFontRenderContext() { return delegate.getFontRenderContext(); }
    @Override public GraphicsConfiguration getDeviceConfiguration() { return delegate.getDeviceConfiguration(); }
    @Override public boolean hit(Rectangle rect, Shape s, boolean onStroke) { return delegate.hit(rect, s, onStroke); }

    // Transforms, in component coordinates
    @Override public void translate(int x, int y) { user.translate(x, y); applyTransform(); }
    @Override public void translate(double x, double y) { user.translate(x, y); applyTransform(); }
    @Override public void rotate(double theta) { user.rotate(theta); applyTransform(); }
    @Override public void rotate(double theta, double x, double y) { user.rotate(theta, x, y); applyTransform(); }
    @Override public void scale(double sx, double sy) { user.scale(sx, sy); applyTransform(); }
    @Override public void shear(double shx, double shy) { user.shear(shx, shy); applyTransform(); }
    @Override public void transform(AffineTransform t) { user.concatenate(t); applyTransform(); }
    @Override public void setTransform(AffineTransform t) { user = new AffineTransform(t); applyTransform(); }
    @Override public AffineTransform getTransform() { return new AffineTransform(user); }

    // Drawing
    @Override public void drawLine(int x1, int y1, int x2, int y2) {
      recordPath(new Line2D.Double(x1, y1, x2, y2), false);
      delegate.drawLine(x1, y1, x2, y2);
    }
    @Override public void drawRect(int x, int y, int w, int h) { recordBox('R', x, y, w, h, 0, false); delegate.drawRect(x, y, w, h); }
    @Override public void fillRect(int x, int y, int w, int h) { recordBox('R', x, y, w, h, 0, true); delegate.fillRect(x, y, w, h); }
    @Override public void clearRect(int x, int y, int w, int h) { warnings.add("clearRect"); delegate.clearRect(x, y, w, h); }
    @Override public void drawRoundRect(int x, int y, int w, int h, int aw, int ah) {
      if (aw != ah) warnings.add("elliptic round rectangle");
      recordBox('R', x, y, w, h, aw / 2.0, false);
      delegate.drawRoundRect(x, y, w, h, aw, ah);
    }
    @Override public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) {
      if (aw != ah) warnings.add("elliptic round rectangle");
      recordBox('R', x, y, w, h, aw / 2.0, true);
      delegate.fillRoundRect(x, y, w, h, aw, ah);
    }
    @Override public void drawOval(int x, int y, int w, int h) { recordBox('O', x, y, w, h, 0, false); delegate.drawOval(x, y, w, h); }
    @Override public void fillOval(int x, int y, int w, int h) { recordBox('O', x, y, w, h, 0, true); delegate.fillOval(x, y, w, h); }
    @Override public void drawArc(int x, int y, int w, int h, int start, int extent) {
      recordPath(new Arc2D.Double(x, y, w, h, start, extent, Arc2D.OPEN), false);
      delegate.drawArc(x, y, w, h, start, extent);
    }
    @Override public void fillArc(int x, int y, int w, int h, int start, int extent) {
      recordPath(new Arc2D.Double(x, y, w, h, start, extent, Arc2D.PIE), true);
      delegate.fillArc(x, y, w, h, start, extent);
    }
    @Override public void drawPolyline(int[] xs, int[] ys, int count) {
      if (count > 0) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) p.lineTo(xs[i], ys[i]);
        recordPath(p, false);
      }
      delegate.drawPolyline(xs, ys, count);
    }
    @Override public void drawPolygon(int[] xs, int[] ys, int count) { recordPath(new Polygon(xs, ys, count), false); delegate.drawPolygon(xs, ys, count); }
    @Override public void fillPolygon(int[] xs, int[] ys, int count) { recordPath(new Polygon(xs, ys, count), true); delegate.fillPolygon(xs, ys, count); }
    @Override public void draw(Shape s) { recordPath(s, false); delegate.draw(s); }
    @Override public void fill(Shape s) { recordPath(s, true); delegate.fill(s); }
    @Override public void drawString(String s, int x, int y) { recordText(s, x, y); delegate.drawString(s, x, y); }
    @Override public void drawString(String s, float x, float y) { recordText(s, x, y); delegate.drawString(s, x, y); }
    @Override public void drawString(AttributedCharacterIterator it, int x, int y) { warnings.add("attributed text"); delegate.drawString(it, x, y); }
    @Override public void drawString(AttributedCharacterIterator it, float x, float y) { warnings.add("attributed text"); delegate.drawString(it, x, y); }
    @Override public void drawGlyphVector(GlyphVector g, float x, float y) { warnings.add("glyph vector"); delegate.drawGlyphVector(g, x, y); }
    @Override public void copyArea(int x, int y, int w, int h, int dx, int dy) { warnings.add("copyArea"); delegate.copyArea(x, y, w, h, dx, dy); }
    @Override public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { warnings.add("image"); return delegate.drawImage(img, xform, obs); }
    @Override public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { warnings.add("image"); delegate.drawImage(img, op, x, y); }
    @Override public void drawRenderedImage(RenderedImage img, AffineTransform xform) { warnings.add("image"); delegate.drawRenderedImage(img, xform); }
    @Override public void drawRenderableImage(RenderableImage img, AffineTransform xform) { warnings.add("image"); delegate.drawRenderableImage(img, xform); }
    @Override public boolean drawImage(Image img, int x, int y, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, x, y, o); }
    @Override public boolean drawImage(Image img, int x, int y, int w, int h, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, x, y, w, h, o); }
    @Override public boolean drawImage(Image img, int x, int y, Color bg, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, x, y, bg, o); }
    @Override public boolean drawImage(Image img, int x, int y, int w, int h, Color bg, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, x, y, w, h, bg, o); }
    @Override public boolean drawImage(Image img, int a, int b, int c, int d, int e, int f, int g, int h, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, a, b, c, d, e, f, g, h, o); }
    @Override public boolean drawImage(Image img, int a, int b, int c, int d, int e, int f, int g, int h, Color bg, ImageObserver o) { warnings.add("image"); return delegate.drawImage(img, a, b, c, d, e, f, g, h, bg, o); }
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && args[0].equals("--state")) {
      state(args);
      return;
    }
    List<String> models = Files.readAllLines(Path.of(args[0])).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    File out = new File(args[1]);
    String variant = args[2];
    double scale = args.length > 3 ? Double.parseDouble(args[3]) : 4.0;
    if (!variant.equals(AppPreferences.SHAPE_SHAPED) && !variant.equals(AppPreferences.SHAPE_RECTANGULAR)) {
      throw new IllegalArgumentException("variant must be shaped or rectangular");
    }
    AppPreferences.GATE_SHAPE.set(variant);
    File images = new File(out, variant);
    images.mkdirs();
    Method setFactory = InstancePainter.class.getDeclaredMethod("setFactory", InstanceFactory.class, AttributeSet.class);
    setFactory.setAccessible(true);
    Class<?> abstractGate = Class.forName("com.cburch.logisim.std.ttl.AbstractTtlGate");
    Method paintInternalBase = abstractGate.getDeclaredMethod("paintInternalBase", InstancePainter.class);
    paintInternalBase.setAccessible(true);
    var internalField = Class.forName("com.cburch.logisim.std.ttl.TtlLibrary").getDeclaredField("DRAW_INTERNAL_STRUCTURE");
    internalField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Attribute<Boolean> drawInternal = (Attribute<Boolean>) internalField.get(null);
    Method paintInternal = abstractGate.getDeclaredMethod("paintInternal", InstancePainter.class, int.class, int.class, int.class, boolean.class);
    try (PrintWriter writer = new PrintWriter(new File(out, variant + ".txt"), "UTF-8")) {
      writer.println("# TtlInternalStructureOracle variant=" + variant + " java=" + System.getProperty("java.version"));
      for (String model : models) {
        InstanceFactory factory = (InstanceFactory) Class.forName("com.cburch.logisim.std.ttl.Ttl" + model).getConstructor().newInstance();
        AttributeSet attrs = factory.createAttributeSet();
        attrs.setValue(drawInternal, true);
        var bounds = factory.getOffsetBounds(attrs);
        int margin = 4;
        int width = (int) Math.ceil((bounds.getWidth() + 2 * margin) * scale);
        int height = (int) Math.ceil((bounds.getHeight() + 2 * margin) * scale);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        AffineTransform base = new AffineTransform();
        base.scale(scale, scale);
        base.translate(margin - bounds.getX(), margin - bounds.getY());
        lines.clear();
        warnings.clear();
        Recorder recorder = new Recorder(g, base, new AffineTransform(), new Font(Font.DIALOG, Font.PLAIN, 12));
        ComponentDrawContext context = new ComponentDrawContext(null, null, null, recorder, recorder);
        InstancePainter painter = new StatelessPainter(context);
        setFactory.invoke(painter, factory, attrs);
        paintInternalBase.invoke(factory, painter);
        g.dispose();
        ImageIO.write(image, "png", new File(images, model + ".png"));
        Method declared = factory.getClass().getMethod("paintInternal", paintInternal.getParameterTypes());
        writer.println("= " + model + " class=" + factory.getClass().getSimpleName() + " paintInternal=" + declared.getDeclaringClass().getSimpleName()
            + " gates=" + TtlDesktopReflect.field(factory, "numberOfGatesToDraw") + " ops=" + lines.stream().filter(l -> !l.startsWith("~")).count()
            + " stateOps=" + lines.stream().filter(l -> l.startsWith("~")).count()
            + (warnings.isEmpty() ? "" : " warnings=" + String.join(",", warnings).replace(' ', '_')));
        lines.forEach(writer::println);
      }
    }
  }

  /**
   * Replays the committed sequential fixtures (core/src/jvmTest/resources/ttl-fixtures) through the desktop model and
   * records, after every row, what the state painters draw: the template of the first row ("~" and "^" operations),
   * then one "row" line per fixture row with the dot texts and fill colours in painting order, and the 74123 timing
   * label colours. The drawing after row SNAPSHOT_ROW goes to state/MODEL.png next to out.txt, at scale 4 with a
   * 4-unit margin. Usage: --state fixtures-dir out.txt model...
   */
  static final int SNAPSHOT_ROW = 100;

  static void state(String[] args) throws Exception {
    File fixtures = new File(args[1]);
    Method setFactory = InstancePainter.class.getDeclaredMethod("setFactory", InstanceFactory.class, AttributeSet.class);
    setFactory.setAccessible(true);
    Method paintInternalBase = Class.forName("com.cburch.logisim.std.ttl.AbstractTtlGate").getDeclaredMethod("paintInternalBase", InstancePainter.class);
    paintInternalBase.setAccessible(true);
    var internalField = Class.forName("com.cburch.logisim.std.ttl.TtlLibrary").getDeclaredField("DRAW_INTERNAL_STRUCTURE");
    internalField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Attribute<Boolean> drawInternal = (Attribute<Boolean>) internalField.get(null);
    BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    try (PrintWriter writer = new PrintWriter(new File(args[2]), "UTF-8")) {
      writer.println("# TtlInternalStructureOracle --state java=" + System.getProperty("java.version"));
      for (int m = 3; m < args.length; m++) {
        String model = args[m];
        InstanceFactory factory = (InstanceFactory) Class.forName("com.cburch.logisim.std.ttl.Ttl" + model).getConstructor().newInstance();
        var clock = new java.util.concurrent.atomic.AtomicLong();
        if (model.equals("74123")) {
          Method setter = factory.getClass().getDeclaredMethod("setClock", java.util.function.LongSupplier.class);
          setter.setAccessible(true);
          setter.invoke(null, (java.util.function.LongSupplier) clock::get);
        }
        List<String> rows = Files.readAllLines(new File(fixtures, model + ".tsv").toPath());
        if (!rows.get(0).equals("# sequential")) throw new IllegalArgumentException(model + " has no sequential fixture");
        int signals = rows.get(1).split("\t")[1].length();
        TtlDesktopOracle.Pins pins = new TtlDesktopOracle.Pins(factory, signals + 2);
        pins.attributes.setValue(drawInternal, true);
        writer.println("= " + model);
        for (int row = 1; row < rows.size(); row++) {
          String stimulus = rows.get(row).split("\t")[1];
          pins.tick = row - 1;
          clock.set((row - 1) * 1_000_000L);
          for (int i = 0; i < signals; i++) pins.inputs[i] = TtlDesktopOracle.val(stimulus.charAt(i));
          pins.inputs[signals] = com.cburch.logisim.data.Value.FALSE;
          pins.inputs[signals + 1] = com.cburch.logisim.data.Value.TRUE;
          factory.propagate(pins.state());
          boolean snapshot = row - 1 == SNAPSHOT_ROW;
          var bounds = factory.getOffsetBounds(pins.attributes);
          BufferedImage image = snapshot ? new BufferedImage((bounds.getWidth() + 8) * 4, (bounds.getHeight() + 8) * 4, BufferedImage.TYPE_INT_ARGB) : scratch;
          Graphics2D g = image.createGraphics();
          AffineTransform base = new AffineTransform();
          if (snapshot) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            base.scale(4, 4);
            base.translate(4 - bounds.getX(), 4 - bounds.getY());
          }
          lines.clear();
          Recorder recorder = new Recorder(g, base, new AffineTransform(), new Font(Font.DIALOG, Font.PLAIN, 12));
          StatelessPainter painter = new StatelessPainter(new ComponentDrawContext(null, null, null, recorder, recorder));
          painter.data = pins.data;
          setFactory.invoke(painter, factory, pins.attributes);
          paintInternalBase.invoke(factory, painter);
          g.dispose();
          if (snapshot) {
            File png = new File(new File(args[2]).getAbsoluteFile().getParentFile(), "state/" + model + ".png");
            png.getParentFile().mkdirs();
            javax.imageio.ImageIO.write(image, "png", png);
          }
          pins.data = painter.data;
          List<String> dynamic = lines.stream().filter(l -> l.startsWith("~") || l.startsWith("^")).toList();
          if (row == 1) dynamic.forEach(l -> writer.println("template " + l));
          List<String> texts = new ArrayList<>(), fills = new ArrayList<>(), labels = new ArrayList<>();
          for (String line : dynamic) {
            String[] f = line.substring(1).split(" ");
            if (line.startsWith("~T")) texts.add(line.substring(1).split(" ", 10)[9]);
            else if (line.startsWith("~O f") || line.startsWith("~P f")) fills.add(f[2]);
            else if (line.startsWith("^T")) labels.add(f[1]);
          }
          writer.println("row " + (row - 1) + "\t" + String.join("", texts) + "\t" + String.join(",", fills) + "\t" + String.join(",", labels));
        }
      }
    }
  }

  /** A placed instance without circuit state, as InstancePainter.getData would see before the first propagation. */
  static final class StatelessPainter extends InstancePainter {
    com.cburch.logisim.instance.InstanceData data;
    StatelessPainter(ComponentDrawContext context) { super(context, null); }
    @Override public com.cburch.logisim.instance.InstanceData getData() { return data; }
    @Override public void setData(com.cburch.logisim.instance.InstanceData value) { data = value; }
  }

  static final class ImageIO {
    static void write(BufferedImage image, String format, File file) throws Exception {
      javax.imageio.ImageIO.write(image, format, file);
    }
  }

  static final class TtlDesktopReflect {
    static Object field(Object target, String name) throws Exception {
      for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
        try {
          var f = type.getDeclaredField(name);
          f.setAccessible(true);
          return f.get(target);
        } catch (NoSuchFieldException ignored) { }
      }
      throw new NoSuchFieldException(name);
    }
  }
}
