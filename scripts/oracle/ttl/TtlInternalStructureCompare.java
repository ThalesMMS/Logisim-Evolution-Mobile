/* SPDX-License-Identifier: GPL-3.0-only */
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Compares the Evolution raster of a TTL internal structure (TtlInternalStructureOracle) with the Mobile raster of
 * the same model (TtlInternalStructureHostTest). Both images use scale 4 and a 4-unit margin around the component's
 * offset bounds, so their pixels share one frame.
 *
 * <p>Ink is a pixel with luminance below 128. Text boxes come from the oracle's display list (centre, baseline,
 * advance width and font size). Outside them, every ink pixel of one image must have ink in the other within
 * TOLERANCE pixels (geometry recall and precision). Inside each text box, both images must have ink (the label is
 * present); glyph shapes differ because Mobile uses its own fonts, so text pixels are reported but not required to
 * match. A composite PNG shows Evolution, Mobile and the overlay (black: both, red: Evolution only, blue: Mobile
 * only).
 *
 * <p>Usage: java TtlInternalStructureCompare display-list.txt[,state.txt] evolution-dir mobile-dir out-dir model...
 * Prints one TSV row per model and writes results.tsv in out-dir. Exits 1 when a model fails.
 */
public final class TtlInternalStructureCompare {
  static final int SCALE = 4;
  static final int MARGIN = 4;
  static final int TOLERANCE = 2;
  static final double REQUIRED = 0.99;

  record TextBox(String text, int left, int top, int right, int bottom) {}

  public static void main(String[] args) throws Exception {
    Map<String, List<TextBox>> texts = new LinkedHashMap<>();
    for (String path : args[0].split(",")) {
      textBoxes(new File(path)).forEach((model, boxes) -> texts.computeIfAbsent(model, k -> new ArrayList<>()).addAll(boxes));
    }
    File evolutionDir = new File(args[1]);
    File mobileDir = new File(args[2]);
    File out = new File(args[3]);
    new File(out, "composites").mkdirs();
    List<String> rows = new ArrayList<>();
    rows.add("model\tresult\tgeometryRecall\tgeometryPrecision\ttextLabels\ttextLabelsPresent\ttextInkEvolution\ttextInkMobile");
    boolean failed = false;
    for (int i = 4; i < args.length; i++) {
      String model = args[i];
      BufferedImage evolution = ImageIO.read(new File(evolutionDir, model + ".png"));
      BufferedImage mobile = ImageIO.read(new File(mobileDir, model + ".png"));
      if (evolution.getWidth() != mobile.getWidth() || evolution.getHeight() != mobile.getHeight()) {
        throw new IllegalStateException(model + ": image sizes differ " + evolution.getWidth() + "x" + evolution.getHeight()
            + " / " + mobile.getWidth() + "x" + mobile.getHeight());
      }
      int w = evolution.getWidth(), h = evolution.getHeight();
      boolean[] a = ink(evolution), b = ink(mobile);
      boolean[] textMask = new boolean[w * h];
      List<TextBox> boxes = texts.getOrDefault(model, List.of());
      int present = 0;
      long textA = 0, textB = 0;
      for (TextBox box : boxes) {
        int inkA = 0, inkB = 0;
        for (int y = Math.max(0, box.top); y < Math.min(h, box.bottom); y++) {
          for (int x = Math.max(0, box.left); x < Math.min(w, box.right); x++) {
            textMask[y * w + x] = true;
            if (a[y * w + x]) inkA++;
            if (b[y * w + x]) inkB++;
          }
        }
        textA += inkA;
        textB += inkB;
        if (inkA > 0 && inkB > 0) present++;
      }
      double recall = covered(a, b, textMask, w, h);
      double precision = covered(b, a, textMask, w, h);
      boolean pass = recall >= REQUIRED && precision >= REQUIRED && present == boxes.size();
      failed |= !pass;
      String row = String.join("\t", model, pass ? "PASS" : "FAIL", fmt(recall), fmt(precision), Integer.toString(boxes.size()),
          Integer.toString(present), Long.toString(textA), Long.toString(textB));
      rows.add(row);
      System.out.println(row);
      ImageIO.write(composite(evolution, mobile, a, b), "png", new File(out, "composites/" + model + ".png"));
    }
    Files.write(new File(out, "results.tsv").toPath(), rows, StandardCharsets.UTF_8);
    if (failed) System.exit(1);
  }

  static String fmt(double v) {
    return String.format(Locale.ROOT, "%.4f", v);
  }

  static boolean[] ink(BufferedImage image) {
    int w = image.getWidth(), h = image.getHeight();
    boolean[] mask = new boolean[w * h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int rgb = image.getRGB(x, y);
        int lum = (((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114) / 1000;
        mask[y * w + x] = lum < 128;
      }
    }
    return mask;
  }

  /** Share of ink pixels of source, outside the text boxes, with ink of target within TOLERANCE pixels. */
  static double covered(boolean[] source, boolean[] target, boolean[] excluded, int w, int h) {
    long total = 0, hit = 0;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int index = y * w + x;
        if (!source[index] || excluded[index]) continue;
        total++;
        search:
        for (int dy = -TOLERANCE; dy <= TOLERANCE; dy++) {
          for (int dx = -TOLERANCE; dx <= TOLERANCE; dx++) {
            int nx = x + dx, ny = y + dy;
            if (nx >= 0 && ny >= 0 && nx < w && ny < h && target[ny * w + nx]) {
              hit++;
              break search;
            }
          }
        }
      }
    }
    return total == 0 ? 1.0 : (double) hit / total;
  }

  static BufferedImage composite(BufferedImage evolution, BufferedImage mobile, boolean[] a, boolean[] b) {
    int w = evolution.getWidth(), h = evolution.getHeight(), gap = 8;
    BufferedImage out = new BufferedImage(w * 3 + gap * 2, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setColor(new Color(0xcccccc));
    g.fillRect(0, 0, out.getWidth(), h);
    g.drawImage(evolution, 0, 0, null);
    g.drawImage(mobile, w + gap, 0, null);
    g.dispose();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = y * w + x;
        int color = a[i] && b[i] ? 0x000000 : a[i] ? 0xd00000 : b[i] ? 0x0050e0 : 0xffffff;
        out.setRGB(2 * (w + gap) + x, y, color);
      }
    }
    return out;
  }

  /** Text boxes in image pixels from the oracle display list ("T argb family size style cx baseline rotation advance text"). */
  static Map<String, List<TextBox>> textBoxes(File displayList) throws Exception {
    Map<String, List<TextBox>> result = new LinkedHashMap<>();
    Map<String, double[]> bounds = new LinkedHashMap<>();
    String model = null;
    for (String raw : Files.readAllLines(displayList.toPath(), StandardCharsets.UTF_8)) {
      // TtlInternalStructureOracle --state prefixes the first row's state operations with "template ".
      String line = raw.startsWith("template ") ? raw.substring("template ".length()) : raw;
      if (line.startsWith("= ")) {
        model = line.split(" ")[1];
        result.put(model, new ArrayList<>());
        continue;
      }
      String op = line.startsWith("^") || line.startsWith("~") ? line.substring(1) : line;
      if (!op.startsWith("T ")) continue;
      String[] f = op.split(" ", 10);
      double size = Double.parseDouble(f[3]);
      double cx = Double.parseDouble(f[5]), baseline = Double.parseDouble(f[6]);
      double rotation = Double.parseDouble(f[7]), advance = Double.parseDouble(f[8]);
      double halfWidth = advance / 2 + 1.5, above = size + 1, below = size * 0.35 + 1;
      double left, top, right, bottom;
      if (Math.abs(rotation) < 1e-6) {
        left = cx - halfWidth; right = cx + halfWidth; top = baseline - above; bottom = baseline + below;
      } else {
        // Quarter turns only: the text runs vertically, its baseline is a vertical line through cx.
        left = cx - above; right = cx + below; top = baseline - halfWidth; bottom = baseline + halfWidth;
        if (rotation > 0) { left = cx - below; right = cx + above; }
      }
      result.get(model).add(new TextBox(f[9], px(left, true), px(top, false), px(right, true) + 1, px(bottom, false) + 1));
    }
    return result;
  }

  // The images start at the offset bounds (x 0, y -30 facing east) minus the margin.
  static int px(double value, boolean horizontal) {
    return (int) Math.floor((value + MARGIN + (horizontal ? 0 : 30)) * SCALE);
  }
}
