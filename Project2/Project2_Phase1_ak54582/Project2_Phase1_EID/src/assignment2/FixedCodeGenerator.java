package assignment2;

import java.util.ArrayList;
import java.util.List;

/**
 * Testing mode: hands out a predetermined list of secrets in order, cycling
 * back to the start after the last one. Each code is checked against the
 * active configuration when used.
 */
public class FixedCodeGenerator implements CodeGenerator {
    private final List<String> codes;
    private int next = 0;

    public FixedCodeGenerator(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("need at least one code");
        }
        this.codes = new ArrayList<>(codes);
    }

    public FixedCodeGenerator(String... codes) {
        this(java.util.Arrays.asList(codes));
    }

    @Override
    public String nextCode(GameConfiguration config) {
        String code = codes.get(next % codes.size());
        next++;
        if (code.length() != config.getPegCount()) {
            throw new IllegalStateException("fixed code '" + code + "' must have "
                    + config.getPegCount() + " pegs");
        }
        for (int i = 0; i < code.length(); i++) {
            if (!config.isLegalColor(code.charAt(i))) {
                throw new IllegalStateException("fixed code '" + code + "' uses illegal color '"
                        + code.charAt(i) + "'");
            }
        }
        return code;
    }
}
