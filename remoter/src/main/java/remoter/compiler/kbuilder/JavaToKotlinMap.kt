package remoter.compiler.kbuilder


val javaToKotlinMap = mapOf(
        "byte" to "kotlin.Byte",
        "short" to "kotlin.Short",
        "int" to "kotlin.Int",
        "long" to "kotlin.Long",
        "char" to "kotlin.Char",
        "float" to "kotlin.Float",
        "double" to "kotlin.Double",
        "boolean" to "kotlin.Boolean",

        "java.lang.Object" to "kotlin.Any",
        "java.lang.Cloneable" to "kotlin.Cloneable",
        "java.lang.Comparable" to "kotlin.Comparable",
        "java.lang.Enum" to "kotlin.Enum",
        "java.lang.Annotation" to "kotlin.Annotation",
        "java.lang.CharSequence" to "kotlin.CharSequence",
        "java.lang.String" to "kotlin.String",
        "java.lang.Number" to "kotlin.Number",
        "java.lang.Throwable" to "kotlin.Throwable",

        "java.lang.Byte" to "kotlin.Byte",
        "java.lang.Short" to "kotlin.Short",
        "java.lang.Integer" to "kotlin.Int",
        "java.lang.Long" to "kotlin.Long",
        "java.lang.Character" to "kotlin.Char",
        "java.lang.Float" to "kotlin.Float",
        "java.lang.Double" to "kotlin.Double",
        "java.lang.Boolean" to "kotlin.Boolean",

        "java.util.Iterator" to "kotlin.collections.MutableIterator",
        "java.lang.Iterable" to "kotlin.collections.MutableIterable",
        "java.util.Collection" to "kotlin.collections.MutableCollection",
        "java.util.Set" to "kotlin.collections.MutableSet",
        "java.util.List" to "kotlin.collections.MutableList",
        "java.util.ListIterator" to "kotlin.collections.ListIterator",
        "java.util.Map" to "kotlin.collections.MutableMap",
        "java.util.Map.Entry" to "kotlin.collections.Map.Entry"
)


