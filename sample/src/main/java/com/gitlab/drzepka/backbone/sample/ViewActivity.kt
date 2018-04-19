package com.gitlab.drzepka.backbone.sample

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import com.gitlab.drzepka.backbone.sample.view.ColorPickerViewFragment
import com.gitlab.drzepka.backbone.sample.view.RoundImageViewFragment
import kotlin.reflect.KClass

/**
 * Activity containg fragments with presentation of each view defined in the core module.
 */
class ViewActivity : AppCompatActivity() {

    private var fragmentExists = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        switchFragment(ListFragment::class)
    }

    fun switchFragment(target: KClass<out Fragment>) {
        val transaction = supportFragmentManager.beginTransaction()
        if (fragmentExists)
            transaction.addToBackStack(null)
        else
            fragmentExists = true

        val fragment = target.java.newInstance()
        transaction.replace(R.id.view_fragment, fragment)
        transaction.commit()

        supportActionBar?.title = fragment.javaClass.simpleName
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportActionBar?.title = "ViewActivity"
    }

    class ListFragment : Fragment() {

        private fun switchFragment(target: KClass<out Fragment>) = (activity as ViewActivity).switchFragment(target)

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val scrollView = ScrollView(context)
            scrollView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            val layout = LinearLayout(context)
            layout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layout.orientation = LinearLayout.VERTICAL
            scrollView.addView(layout)

            // Add all known fragments to list
            FRAGMENTS.forEach {
                val button = Button(context)
                button.text = it.java.simpleName
                button.setOnClickListener { _ ->
                    switchFragment(it)
                }

                layout.addView(button)
            }

            return scrollView
        }
    }

    companion object {
        /** Fragments available in list */
        private val FRAGMENTS = listOf(RoundImageViewFragment::class, ColorPickerViewFragment::class)
    }
}