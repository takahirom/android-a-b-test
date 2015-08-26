package takahirom.com.android_a_b_test;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * This is main class of ABTest.
 * You can create ABTest instance by ABTest.Builder
 */
public class ABTest<T extends Enum<T>> {

    private final ABPattern<T> pattern;

    private ABTest(ABPattern<T> pattern) {
        this.pattern = pattern;
    }

    public void visit(VisitDispatcher<T> visitDispatcher) {
        visitDispatcher.dispatch(pattern);
    }

    public void convert(ConvertDispatcher<T> convertDispatcher) {
        convertDispatcher.dispatch(pattern);
    }

    /**
     * If ABTest already built,ABTest instance can created by patternEnumValue.
     * @param context
     * @param clazz clazz is Pattern Enum class
     * @param <T> T is Pattern Enum type
     * @return ABTest instance. If ABTest doesn't built yet, returns null
     */
    public static <T extends Enum<T>> ABTest<T> getBuiltInstance(Context context, Class<T> clazz) {
        SharedPreferences sharedPreferences = getABTestPreferences(context);
        String savedPatternName = sharedPreferences.getString(clazz.getName(), null);
        EnumSet<T> enumSet = EnumSet.allOf(clazz);
        for (T patternEnumValue : enumSet) {
            if (patternEnumValue.name().equalsIgnoreCase(savedPatternName)) {
                ABPattern<T> abPattern = new ABPattern<>(patternEnumValue);
                return new ABTest<>(abPattern);
            }
        }
        return null;
    }

    private static SharedPreferences getABTestPreferences(Context context) {
        return context.getSharedPreferences("ab_test_patterns", Context.MODE_PRIVATE);
    }

    public static class Builder<T extends Enum<T>> {

        private final Context context;
        private List<ABPattern<T>> patterns = new ArrayList<>();
        private Class<T> clazz;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder<T> withClass(Class<T> clazz) {
            this.clazz = clazz;
            return this;
        }

        public Builder<T> addPattern(ABPattern<T> abPattern) {
            patterns.add(abPattern);
            return this;
        }

        public ABTest<T> buildIfFirstTime() {
            ABTest<T> builtInstance = ABTest.getBuiltInstance(context, clazz);
            if (builtInstance != null) {
                return builtInstance;
            }
            return build();
        }

        public ABTest<T> build() {
            ABPattern<T> resultPattern = chooseRandomABPattern();
            getABTestPreferences(context).edit().putString(clazz.getName(), resultPattern.getName()).apply();
            return new ABTest<>(resultPattern);
        }

        private ABPattern<T> chooseRandomABPattern() {
            int sum = 0;
            for (ABPattern<T> pattern : patterns) {
                sum += pattern.weight;
            }
            Random r = new Random();
            int randomInt = r.nextInt(sum);

            int countUp = 0;
            ABPattern<T> resultPattern = null;
            for (ABPattern<T> pattern : patterns) {
                countUp += pattern.weight;
                if (randomInt < countUp) {
                    resultPattern = pattern;
                    break;
                }
            }
            return resultPattern;
        }
    }
}
