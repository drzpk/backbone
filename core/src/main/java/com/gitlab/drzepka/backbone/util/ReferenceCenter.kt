package com.gitlab.drzepka.backbone.util

import java.util.*

/**
 * Reference center is a place that stores references to various objects. It uses weak reference for storing,
 * so **only non-anonymous objects can be stored here**. Attempt to do otherwise will result in generating
 * an exception.
 */
object ReferenceCenter {
    /** Holds all registered references. */
    private val references = WeakHashMap<Any, Int>()

    /**
     * Registers new references and returns its ID by which it can be obtained later. Only **non-anonymous** objects
     * can be stored!
     */
    fun registerReference(obj: Any): Int {
        if(obj::class.java.isAnonymousClass)
            throw RuntimeException("Only object created from non-anonymous classes can be registered as references")

        var id: Int
        val random = Random()
        do {
            id = random.nextInt()
        } while (references.containsValue(id))

        references[obj] = id
        return id
    }

    /**
     * Returns previously saved reference associated with given ID. If no such reference can be found - a `null` value
     * is returned.
     */
    fun getReference(id: Int): Any? {
        for (entry in references.entries) {
            if (entry.value == id)
                return entry.key
        }

        return null
    }
}