package com.vunv.n5nihongo.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vunv.n5nihongo.R

class LessonAdapter(
    private val lessons: List<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvLessonNumber)
        val tvName: TextView = view.findViewById(R.id.tvLessonName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lessonNum = lessons[position]
        holder.tvNumber.text = lessonNum.toString()
        holder.tvName.text = "Bài học số $lessonNum"

        holder.itemView.setOnClickListener { onItemClick(lessonNum) }
    }

    override fun getItemCount() = lessons.size
}