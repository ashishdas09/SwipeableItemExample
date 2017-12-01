package com.ashishdas.example.swipeableitem;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.ashishdas.example.swipeableitem.layout.SwipeableLayout;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
	private RecyclerView recyclerView;
	private MyAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		ArrayList arrayList = new ArrayList<>();
		for (int i = 0; i < 25; i++)
		{
			arrayList.add(String.valueOf(i + 1) + " DateTime : " +  new Date());
		}

		adapter = new MyAdapter(SwipeableLayout.DragEdge.RIGHT, arrayList);
		recyclerView.setAdapter(adapter);

		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Only if you need to restore open/close state when
		// the orientation is changed
		if (adapter != null) {
			adapter.saveStates(outState);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Only if you need to restore open/close state when
		// the orientation is changed
		if (adapter != null) {
			adapter.restoreStates(savedInstanceState);
		}
	}
}
