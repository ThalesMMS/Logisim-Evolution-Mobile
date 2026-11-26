import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.arith.floating.FpAbsolute;
import com.cburch.logisim.std.arith.floating.FpAbsoluteHdlGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Writes the module body that Evolution's FP Absolute HDL generator (PR 3174) emits for each width
 * and language, plus its module name, so run.py can wrap and simulate it.
 *
 * <p>Usage: {@code FpAbsoluteEvolutionOracle output-directory}. Run with
 * {@code -Djava.util.prefs.PreferencesFactory=FpAbsoluteEvolutionOracle$MemoryPreferencesFactory} so
 * the HDL language preference is not written to the user's preferences.
 */
public final class FpAbsoluteEvolutionOracle {
    public static void main(String[] args) throws Exception {
        final var out = Path.of(args[0]);
        Files.createDirectories(out);
        for (final var language : new String[] {"VHDL", "Verilog"}) {
            AppPreferences.HdlType.set(language);
            for (final int width : new int[] {8, 16, 32, 64}) {
                final var factory = new FpAbsolute();
                final var attrs = factory.createAttributeSet();
                attrs.setValue(StdAttr.FP_WIDTH, BitWidth.create(width));
                final var body = new FpAbsoluteHdlGeneratorFactory().getModuleFunctionality(null, attrs);
                final var name = factory.getHDLName(attrs);
                final var file = out.resolve(language.toLowerCase() + "-" + width + ".body");
                Files.writeString(file, name + "\n" + String.join("\n", body.get()) + "\n");
            }
        }
    }

    public static final class MemoryPreferencesFactory implements PreferencesFactory {
        private final Preferences user = new MemoryPreferences(null, "");
        private final Preferences system = new MemoryPreferences(null, "");
        @Override public Preferences userRoot() { return user; }
        @Override public Preferences systemRoot() { return system; }
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();
        MemoryPreferences(AbstractPreferences parent, String name) { super(parent, name); }
        @Override protected void putSpi(String key, String value) { values.put(key, value); }
        @Override protected String getSpi(String key) { return values.get(key); }
        @Override protected void removeSpi(String key) { values.remove(key); }
        @Override protected void removeNodeSpi() { values.clear(); children.clear(); }
        @Override protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
        @Override protected String[] childrenNamesSpi() { return children.keySet().toArray(String[]::new); }
        @Override protected AbstractPreferences childSpi(String name) { return children.computeIfAbsent(name, n -> new MemoryPreferences(this, n)); }
        @Override protected void syncSpi() {}
        @Override protected void flushSpi() {}
    }
}
