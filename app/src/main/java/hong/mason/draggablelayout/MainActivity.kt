package hong.mason.draggablelayout

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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
                Toast.makeText(baseContext, "onExpanded", Toast.LENGTH_SHORT).show()
            }

            override fun onCollapsed() {
                Toast.makeText(baseContext, "onCollapsed", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
