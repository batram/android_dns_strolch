package de.batram.dnsstrolch.ui

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.samsao.swipeablelstitem.views.SwipeableView

class ListAdapter(
    private val mContext: Context,
    data: MutableList<Filter>,
    cbLeft: (Filter) -> (Boolean),
    cbRight: (Filter) -> (Boolean)
) :
    RecyclerView.Adapter<ListAdapter.Holder>() {

    var cbLeft: (Filter) -> (Boolean) = cbLeft;
    var cbRight: (Filter) -> (Boolean) = cbRight;

    private val mData: MutableList<Filter> = data

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        val view = ListItem(mContext)
        return Holder(view)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        holder.setup(mData[position])
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class Holder(itemView: ListItem) : RecyclerView.ViewHolder(itemView) {
        val mListItem: ListItem = itemView
        fun setup(filter: Filter) {
            mListItem.resetSwipe() //to avoid problems with cell recycling
            mListItem.setup(filter)
            mListItem.setLeftSwipeListener(SwipeableView.LeftSwipeListener {
                mListItem.resetSwipe()
                if (cbLeft(filter)){
                    removeItem(filter)
                }
            })
            mListItem.setRightSwipeListener(SwipeableView.RightSwipeListener {
                mListItem.resetSwipe()
                if (cbRight(filter)){
                    removeItem(filter)
                }
            })
        }

    }

    fun removeItem(filter: Filter) {
        for (i in mData.indices) {
            if (mData[i] == filter) {
                mData.removeAt(i)
                notifyItemRemoved(i)
                break
            }
        }
    }

}