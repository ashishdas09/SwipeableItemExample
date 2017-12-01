package com.ashishdas.example.swipeableitem.layout;

import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SwipeableLayoutBinderHelper
{
	private static final String BUNDLE_MAP_KEY = "SwipeViewBinderHelper_Bundle_Map_Key";

	private Map<String, SwipeableLayout.State> mapStates = Collections.synchronizedMap(new HashMap<String, SwipeableLayout.State>());
	private Map<String, SwipeableLayout> mapLayouts = Collections.synchronizedMap(new HashMap<String, SwipeableLayout>());
	private Set<String> lockedSwipeSet = Collections.synchronizedSet(new HashSet<String>());

	private volatile boolean openOnlyOne = false;
	private final Object stateChangeLock = new Object();

	public void bind(final SwipeableLayout swipeLayout, final String id)
	{
		if (swipeLayout.shouldRequestLayout())
		{
			swipeLayout.requestLayout();
		}

		mapLayouts.values().remove(swipeLayout);
		mapLayouts.put(id, swipeLayout);

		swipeLayout.abort();
		swipeLayout.setOnDragStateChangeListener(new SwipeableLayout.OnDragStateChangeListener()
		{
			@Override
			public void onDragStateChanged(SwipeableLayout.State state)
			{
				mapStates.put(id, state);

				if (openOnlyOne)
				{
					closeOthers(id, swipeLayout);
				}
			}
		});

		// first time binding.
		if (!mapStates.containsKey(id))
		{
			mapStates.put(id, SwipeableLayout.State.CLOSE);
			swipeLayout.close(false);
		}

		// not the first time, then close or open depends on the current state.
		else
		{
			SwipeableLayout.State state = mapStates.get(id);

			if (state == SwipeableLayout.State.CLOSE
					|| state == SwipeableLayout.State.CLOSING
					|| state == SwipeableLayout.State.DRAGGING)
			{
				swipeLayout.close(false);
			}
			else
			{
				swipeLayout.open(false);
			}
		}

		// set lock swipe
		swipeLayout.setLockDrag(lockedSwipeSet.contains(id));
	}

	/**
	 * Only if you need to restore open/close state when the orientation is changed.
	 * Call this method in {@link android.app.Activity#onSaveInstanceState(Bundle)}
	 */
	public void saveStates(Bundle outState)
	{
		if (outState == null)
		{
			return;
		}

		Bundle statesBundle = new Bundle();
		for (Map.Entry<String, SwipeableLayout.State> entry : mapStates.entrySet())
		{
			statesBundle.putInt(entry.getKey(), entry.getValue().getInt());
		}
		outState.putBundle(BUNDLE_MAP_KEY, statesBundle);
	}


	/**
	 * Only if you need to restore open/close state when the orientation is changed.
	 * Call this method in {@link android.app.Activity#onRestoreInstanceState(Bundle)}
	 */
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public void restoreStates(Bundle inState)
	{
		if (inState == null)
		{
			return;
		}

		if (inState.containsKey(BUNDLE_MAP_KEY))
		{
			HashMap<String, SwipeableLayout.State> restoredMap = new HashMap<>();

			Bundle statesBundle = inState.getBundle(BUNDLE_MAP_KEY);
			Set<String> keySet = statesBundle.keySet();

			if (keySet != null)
			{
				for (String key : keySet)
				{
					restoredMap.put(key, SwipeableLayout.State.fromInt(statesBundle.getInt(key)));
				}
			}

			mapStates = restoredMap;
		}
	}

	/**
	 * Lock swipe for some layouts.
	 *
	 * @param id a string that uniquely defines the data object.
	 */
	public void lockSwipe(String... id)
	{
		setLockSwipe(true, id);
	}

	/**
	 * Unlock swipe for some layouts.
	 *
	 * @param id a string that uniquely defines the data object.
	 */
	public void unlockSwipe(String... id)
	{
		setLockSwipe(false, id);
	}

	/**
	 * @param openOnlyOne If set to true, then only one row can be opened at a time.
	 */
	public void setOpenOnlyOne(boolean openOnlyOne)
	{
		this.openOnlyOne = openOnlyOne;
	}

	/**
	 * Open a specific layout.
	 *
	 * @param id unique id which identifies the data object which is bind to the layout.
	 */
	public void openLayout(final String id)
	{
		synchronized (stateChangeLock)
		{
			mapStates.put(id, SwipeableLayout.State.OPEN);

			if (mapLayouts.containsKey(id))
			{
				final SwipeableLayout layout = mapLayouts.get(id);
				layout.open(true);
			}
			else if (openOnlyOne)
			{
				closeOthers(id, mapLayouts.get(id));
			}
		}
	}

	/**
	 * Close a specific layout.
	 *
	 * @param id unique id which identifies the data object which is bind to the layout.
	 */
	public void closeLayout(final String id)
	{
		synchronized (stateChangeLock)
		{
			mapStates.put(id, SwipeableLayout.State.CLOSE);

			if (mapLayouts.containsKey(id))
			{
				final SwipeableLayout layout = mapLayouts.get(id);
				layout.close(true);
			}
		}
	}

	/**
	 * Close all.
	 */
	public void closeAll()
	{
		synchronized (stateChangeLock)
		{
			// close other rows if openOnlyOne is true.
			if (getOpenCount() > 0)
			{
				for (SwipeableLayout layout : mapLayouts.values())
				{
					layout.close(true);
				}
			}
		}
	}

	private void closeOthers(String id, SwipeableLayout swipeLayout)
	{
		synchronized (stateChangeLock)
		{
			// close other rows if openOnlyOne is true.
			if (getOpenCount() > 1)
			{
				for (Map.Entry<String, SwipeableLayout.State> entry : mapStates.entrySet())
				{
					if (!entry.getKey().equals(id))
					{
						entry.setValue(SwipeableLayout.State.CLOSE);
					}
				}

				for (SwipeableLayout layout : mapLayouts.values())
				{
					if (layout != swipeLayout)
					{
						layout.close(true);
					}
				}
			}
		}
	}

	private void setLockSwipe(boolean lock, String... id)
	{
		if (id == null || id.length == 0)
		{
			return;
		}

		if (lock)
		{
			lockedSwipeSet.addAll(Arrays.asList(id));
		}
		else
		{
			lockedSwipeSet.removeAll(Arrays.asList(id));
		}

		for (String s : id)
		{
			SwipeableLayout layout = mapLayouts.get(s);
			if (layout != null)
			{
				layout.setLockDrag(lock);
			}
		}
	}

	private int getOpenCount()
	{
		int total = 0;

		for (SwipeableLayout.State state : mapStates.values())
		{
			if (state == SwipeableLayout.State.OPEN || state == SwipeableLayout.State.OPENING)
			{
				total++;
			}
		}
		return total;
	}
}
