package cn.iinti.atom.utils

import org.apache.commons.lang3.ClassUtils
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Member
import java.lang.reflect.Modifier

/**
 * Contains common code for working with Methods/Constructors, extracted and
 * refactored from <code>MethodUtils</code> when it was imported from Commons
 * BeanUtils.
 *
 * @version $Id: MemberUtils.java 1143537 2011-07-06 19:30:22Z joehni $
 * @since 2.5
 */
abstract class MemberUtils {
    companion object {
        private const val ACCESS_TEST = Modifier.PUBLIC or Modifier.PROTECTED or Modifier.PRIVATE

        /**
         * Array of primitive number types ordered by "promotability"
         */
        private val ORDERED_PRIMITIVE_TYPES = arrayOf(Byte::class.javaPrimitiveType, Short::class.javaPrimitiveType,
            Character::class.javaPrimitiveType, Integer::class.javaPrimitiveType, Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, Double::class.javaPrimitiveType)

        /**
         * XXX Default access superclass workaround
         * <p>
         * When a public class has a default access superclass with public members,
         * these members are accessible. Calling them from compiled code works fine.
         * Unfortunately, on some JVMs, using reflection to invoke these members
         * seems to (wrongly) prevent access even when the modifier is public.
         * Calling setAccessible(true) solves the problem but will only work from
         * sufficiently privileged code. Better workarounds would be gratefully
         * accepted.
         *
         * @param o the AccessibleObject to set as accessible
         */
        fun setAccessibleWorkaround(o: AccessibleObject?) {
            if (o == null || o.isAccessible) {
                return
            }
            val m = o as Member
            if (Modifier.isPublic(m.modifiers) && isPackageAccess(m.declaringClass.modifiers)) {
                try {
                    o.isAccessible = true
                } catch (e: SecurityException) { // NOPMD
                    // ignore in favor of subsequent IllegalAccessException
                }
            }
        }

        /**
         * Returns whether a given set of modifiers implies package access.
         *
         * @param modifiers to test
         * @return true unless package/protected/private modifier detected
         */
        fun isPackageAccess(modifiers: Int): Boolean {
            return modifiers and ACCESS_TEST == 0
        }

        /**
         * Returns whether a Member is accessible.
         *
         * @param m Member to check
         * @return true if <code>m</code> is accessible
         */
        fun isAccessible(m: Member?): Boolean {
            return m != null && Modifier.isPublic(m.modifiers) && !m.isSynthetic
        }

        /**
         * Compares the relative fitness of two sets of parameter types in terms of
         * matching a third set of runtime parameter types, such that a list ordered
         * by the results of the comparison would return the best match first
         * (least).
         *
         * @param left   the "left" parameter set
         * @param right  the "right" parameter set
         * @param actual the runtime parameter types to match against
         *               <code>left</code>/<code>right</code>
         * @return int consistent with <code>compare</code> semantics
         */
        fun compareParameterTypes(left: Array<Class<*>>, right: Array<Class<*>>, actual: Array<Class<*>>): Int {
            val leftCost = getTotalTransformationCost(actual, left)
            val rightCost = getTotalTransformationCost(actual, right)
            return if (leftCost < rightCost) -1 else if (rightCost < leftCost) 1 else 0
        }

        /**
         * Returns the sum of the object transformation cost for each class in the
         * source argument list.
         *
         * @param srcArgs  The source arguments
         * @param destArgs The destination arguments
         * @return The total transformation cost
         */
        private fun getTotalTransformationCost(srcArgs: Array<Class<*>>, destArgs: Array<Class<*>>): Float {
            var totalCost = 0.0f
            for (i in srcArgs.indices) {
                totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i])
            }
            return totalCost
        }

        /**
         * Gets the number of steps required needed to turn the source class into
         * the destination class. This represents the number of steps in the object
         * hierarchy graph.
         *
         * @param srcClass  The source class
         * @param destClass The destination class
         * @return The cost of transforming an object
         */
        private fun getObjectTransformationCost(srcClass: Class<*>, destClass: Class<*>): Float {
            if (destClass.isPrimitive) {
                return getPrimitivePromotionCost(srcClass, destClass)
            }
            var cost = 0.0f
            var currentSrcClass = srcClass
            while (currentSrcClass != null && currentSrcClass != destClass) {
                if (destClass.isInterface && ClassUtils.isAssignable(currentSrcClass, destClass, true)) {
                    // slight penalty for interface match.
                    // we still want an exact match to override an interface match,
                    // but
                    // an interface match should override anything where we have to
                    // get a superclass.
                    cost += 0.25f
                    break
                }
                cost += 1.0f
                currentSrcClass = currentSrcClass.superclass
            }
            /*
             * If the destination class is null, we've travelled all the way up to
             * an Object match. We'll penalize this by adding 1.5 to the cost.
             */
            if (currentSrcClass == null) {
                cost += 1.5f
            }
            return cost
        }

        /**
         * Gets the number of steps required to promote a primitive number to another
         * type.
         *
         * @param srcClass  the (primitive) source class
         * @param destClass the (primitive) destination class
         * @return The cost of promoting the primitive
         */
        private fun getPrimitivePromotionCost(srcClass: Class<*>, destClass: Class<*>): Float {
            var cost = 0.0f
            var cls = srcClass
            if (!cls.isPrimitive) {
                // slight unwrapping penalty
                cost += 0.1f
                cls = ClassUtils.wrapperToPrimitive(cls)
            }
            for (i in 0 until ORDERED_PRIMITIVE_TYPES.size) {
                if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                    cost += 0.1f
                    if (i < ORDERED_PRIMITIVE_TYPES.size - 1) {
                        cls = ORDERED_PRIMITIVE_TYPES[i + 1]!!
                    }
                }
            }
            return cost
        }
    }
}