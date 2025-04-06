package lean.nullability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>This annotation can be applied to a package, class or method to indicate that the class fields, method
 * parameters and return types are {@link Nonnull}</p>
 * <p>This declares it by default, allowing the override of configurations when:</p>
 * <ul>
 * <li>There's an explicit nullness annotation (e.g. {@link Nullable})</li>
 * <li>The method overrides a method in a superclass (in which case the annotation of the corresponding parameter in
 * the superclass applies)</li>
 * <li>There is a default parameter annotation (like {@link ParametersAreNullableByDefault}) applied to a more
 * tightly nested element.</li>
 * </ul>
 * <p>Note: this annotation was created for annotating all packages in order to achieve an explicit nullability
 * configuration</p>
 *
 * @see Nonnull
 * @see Nullable
 * @see ParametersAreNullableByDefault
 * @see ParametersAreNonnullByDefault
 */
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonnullByDefault {
}
