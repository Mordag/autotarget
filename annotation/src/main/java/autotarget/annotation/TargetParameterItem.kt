package autotarget.annotation

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TargetParameterItem(val key: String,
                                 val name: String = "unspecified",
                                 val type: KClass<*>,
                                 val optional: Boolean = false)