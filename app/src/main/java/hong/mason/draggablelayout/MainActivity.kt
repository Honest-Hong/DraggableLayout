package hong.mason.draggablelayout

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonExpand.setOnClickListener{
            draggableLayout.expand()
        }
        buttonCollapse.setOnClickListener {
            draggableLayout.collapse()
        }

        draggableLayout.addCallback(object : DraggableLayout.Callback {
            override fun onExpanded() {
                buttonExpand.visibility = View.GONE
                buttonCollapse.visibility = View.VISIBLE
            }

            override fun onCollapsed() {
                buttonExpand.visibility = View.VISIBLE
                buttonCollapse.visibility = View.GONE
            }
        })
    }
}
