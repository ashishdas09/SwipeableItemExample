package com.ashishdas.example.swipeableitem;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ashishdas.example.swipeableitem.layout.SwipeableLayout;
import com.ashishdas.example.swipeableitem.layout.SwipeableLayoutBinderHelper;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
	private static final String LOG_TAG = "SwipeAdapter";

	private SwipeableLayout.DragEdge mDragEdge;
	private List<String> mArrayList;
	private final SwipeableLayoutBinderHelper mBinderhelper;

	public MyAdapter(SwipeableLayout.DragEdge dragEdge, ArrayList<String> arrayList)
	{
		mArrayList = arrayList;
		mDragEdge = dragEdge;
		mBinderhelper = new SwipeableLayoutBinderHelper();
		mBinderhelper.setOpenOnlyOne(true);
	}

	@Override
	public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_swipe, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public final void onBindViewHolder(RecyclerView.ViewHolder holder, final int position)
	{
		ViewHolder viewHolder = (ViewHolder) holder;
		mBinderhelper.bind(viewHolder.swipeLayout, position + "");
		viewHolder.bind(mArrayList.get(position));
	}

	@Override
	public int getItemCount()
	{
		return mArrayList.size();
	}

	@Override
	public long getItemId(int position)
	{
		return super.getItemId(position);
	}

	/**
	 * Only if you need to restore open/close state when the orientation is changed.
	 * Call this method in {@link android.app.Activity#onSaveInstanceState(Bundle)}
	 */
	public void saveStates(Bundle outState)
	{
		mBinderhelper.saveStates(outState);
	}

	/**
	 * Only if you need to restore open/close state when the orientation is changed.
	 * Call this method in {@link android.app.Activity#onRestoreInstanceState(Bundle)}
	 */
	public void restoreStates(Bundle inState)
	{
		mBinderhelper.restoreStates(inState);
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SwipeableLayout.OnSwipeListener
	{
		private SwipeableLayout swipeLayout;
		private TextView textView;
		private View menuOption1, menuOption2;

		public ViewHolder(View view)
		{
			super(view);
			swipeLayout = (SwipeableLayout) view.findViewById(R.id.swipeLayout);
			swipeLayout.setDragEdge(mDragEdge);
			textView = (TextView) view.findViewById(R.id.textView);
			menuOption1 = view.findViewById(R.id.iv_menu_option1);
			menuOption2 = view.findViewById(R.id.iv_menu_option2);
		}

		public void bind(String txt)
		{
			swipeLayout.setOnSwipeListener(this);
			menuOption1.setOnClickListener(null);
			menuOption2.setOnClickListener(null);

			textView.setText(txt);
		}

		@Override
		public void onClick(final View v)
		{
			switch (v.getId())
			{
				case R.id.iv_menu_option1:
					Toast.makeText(v.getContext(), "onClick - menuOption1", Toast.LENGTH_SHORT).show();
					swipeLayout.close(true);
				break;
				case R.id.iv_menu_option2:
					Toast.makeText(v.getContext(), "onClick - menuOption2", Toast.LENGTH_SHORT).show();
					swipeLayout.close(true);
				break;
			}
		}

		@Override
		public void onHalfSwipe(final SwipeableLayout view, final boolean isOpened)
		{
			Log.i(LOG_TAG, "onHalfSwipe - isOpened "+isOpened);
			if(isOpened)
			{
				menuOption1.setOnClickListener(this);
				menuOption2.setOnClickListener(this);
			}
		}

		@Override
		public void onFullSwipe(final SwipeableLayout view)
		{
			Log.i(LOG_TAG, "onFullSwipe");
			mBinderhelper.closeAll();
		}
	}
}

