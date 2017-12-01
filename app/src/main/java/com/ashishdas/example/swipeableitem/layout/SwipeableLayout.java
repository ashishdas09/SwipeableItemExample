package com.ashishdas.example.swipeableitem.layout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

public class SwipeableLayout extends ViewGroup
{
	private static final String TAG = "SwipeableItem";

	public interface OnSwipeListener
	{
		void onHalfSwipe(SwipeableLayout view, boolean isOpened);

		void onFullSwipe(SwipeableLayout view);
	}

	interface OnDragStateChangeListener
	{
		void onDragStateChanged(State state);
	}

	public enum DragEdge
	{
		LEFT, RIGHT
	}

	public enum State
	{
		CLOSE(0), CLOSING(1), OPEN(2), OPENING(3), DRAGGING(4);
		private int mStateCode;

		State(int statusCode)
		{
			mStateCode = statusCode;
		}

		public int getInt()
		{
			return mStateCode;
		}

		public static State fromInt(int statusCode)
		{
			for (State status : values())
			{
				if (status.getInt() == statusCode)
				{
					return status;
				}
			}
			return CLOSE;
		}
	}

	private static final int DEFAULT_MIN_FLING_VELOCITY = 300;

	private View mMainView;
	private View mSecondaryView;

	private Rect mRectMainClose = new Rect();
	private Rect mRectMainOpen = new Rect();
	private Rect mRectSecClose = new Rect();
	private Rect mRectSecOpen = new Rect();

	private State mState = State.CLOSE;
	private DragEdge mDragEdge = DragEdge.RIGHT;

	private boolean mIsOpenBeforeInit = false;
	private volatile boolean mAborted = false;
	private volatile boolean mLockDrag = false;

	private int mTouchSlop;
	private int mOnLayoutCount = 0;
	private int mHorizontalDragRange;

	private final ViewDragHelper mDragHelper;

	private OnSwipeListener mOnSwipeListener;
	private OnDragStateChangeListener mOnDragStateChangeListener;

	public SwipeableLayout(Context context)
	{
		this(context, null);
	}

	public SwipeableLayout(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public SwipeableLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);

		// scroll threshold
		ViewConfiguration vc = ViewConfiguration.get(this.getContext());
		mTouchSlop = vc.getScaledTouchSlop();

		mDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
	}

	/**
	 * Only used for {@link SwipeableLayoutBinderHelper}
	 */
	void setOnDragStateChangeListener(OnDragStateChangeListener listener)
	{
		mOnDragStateChangeListener = listener;
	}

	@Override
	protected boolean checkLayoutParams(LayoutParams p)
	{
		return p instanceof LayoutParams;
	}

	/**
	 * @param lock if set to true, the user cannot drag/swipe the layout.
	 */
	public void setLockDrag(boolean lock)
	{
		mLockDrag = lock;
	}

	/**
	 * @return true if the drag/swipe motion is currently locked.
	 */
	public boolean isDragLocked()
	{
		return mLockDrag;
	}

	/**
	 * In RecyclerView/ListView, onLayout should be called 2 times to display children views correctly.
	 * This method check if it've already called onLayout two times.
	 *
	 * @return true if you should call {@link #requestLayout()}.
	 */
	protected boolean shouldRequestLayout()
	{
		return mOnLayoutCount < 2;
	}

	/**
	 * Abort current motion in progress. Only used for {@link SwipeableLayoutBinderHelper}
	 */
	protected void abort()
	{
		mAborted = true;
		mDragHelper.abort();
	}

	/**
	 * Set the edge where the layout can be dragged from.
	 *
	 * @param dragEdge
	 */
	public void setDragEdge(DragEdge dragEdge)
	{
		mDragEdge = dragEdge;
	}

	/**
	 * @return true if layout is fully opened, false otherwise.
	 */
	public boolean isOpened()
	{
		return (mState == State.OPEN);
	}

	/**
	 * @return true if layout is fully closed, false otherwise.
	 */
	public boolean isClosed()
	{
		return (mState == State.CLOSE);
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		if (getChildCount() >= 2)
		{
			mSecondaryView = getChildAt(0);
			mMainView = getChildAt(1);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		if (getChildCount() < 2)
		{
			throw new RuntimeException("Layout must have two children");
		}

		final LayoutParams params = getLayoutParams();

		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int desiredWidth = 0;
		int desiredHeight = 0;

		// first find the largest child
		for (int i = 0; i < getChildCount(); i++)
		{
			final View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			desiredWidth = Math.max(child.getMeasuredWidth(), desiredWidth);
			desiredHeight = Math.max(child.getMeasuredHeight(), desiredHeight);
		}
		// create new measure spec using the largest child width
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, widthMode);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, heightMode);

		final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		final int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

		for (int i = 0; i < getChildCount(); i++)
		{
			final View child = getChildAt(i);
			final LayoutParams childParams = child.getLayoutParams();

			if (childParams != null)
			{
				if (childParams.height == LayoutParams.MATCH_PARENT)
				{
					child.setMinimumHeight(measuredHeight);
				}

				if (childParams.width == LayoutParams.MATCH_PARENT)
				{
					child.setMinimumWidth(measuredWidth);
				}
			}

			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			desiredWidth = Math.max(child.getMeasuredWidth(), desiredWidth);
			desiredHeight = Math.max(child.getMeasuredHeight(), desiredHeight);
		}

		// taking accounts of padding
		desiredWidth += getPaddingLeft() + getPaddingRight();
		desiredHeight += getPaddingTop() + getPaddingBottom();

		// adjust desired width
		if (widthMode == MeasureSpec.EXACTLY)
		{
			desiredWidth = measuredWidth;
		}
		else
		{
			if (params.width == LayoutParams.MATCH_PARENT)
			{
				desiredWidth = measuredWidth;
			}

			if (widthMode == MeasureSpec.AT_MOST)
			{
				desiredWidth = (desiredWidth > measuredWidth) ? measuredWidth : desiredWidth;
			}
		}

		// adjust desired height
		if (heightMode == MeasureSpec.EXACTLY)
		{
			desiredHeight = measuredHeight;
		}
		else
		{
			if (params.height == LayoutParams.MATCH_PARENT)
			{
				desiredHeight = measuredHeight;
			}

			if (heightMode == MeasureSpec.AT_MOST)
			{
				desiredHeight = (desiredHeight > measuredHeight) ? measuredHeight : desiredHeight;
			}
		}

		setMeasuredDimension(desiredWidth, desiredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		mAborted = false;

		for (int index = 0; index < getChildCount(); index++)
		{
			final View child = getChildAt(index);

			int left, right, top, bottom;
			left = right = top = bottom = 0;

			final int minLeft = getPaddingLeft();
			final int maxRight = Math.max(r - getPaddingRight() - l, 0);
			final int minTop = getPaddingTop();
			final int maxBottom = Math.max(b - getPaddingBottom() - t, 0);

			int measuredChildHeight = child.getMeasuredHeight();
			int measuredChildWidth = child.getMeasuredWidth();

			// need to take account if child size is match_parent
			final LayoutParams childParams = child.getLayoutParams();
			boolean matchParentHeight = false;
			boolean matchParentWidth = false;

			if (childParams != null)
			{
				matchParentHeight = (childParams.height == LayoutParams.MATCH_PARENT) ||
						(childParams.height == LayoutParams.FILL_PARENT);
				matchParentWidth = (childParams.width == LayoutParams.MATCH_PARENT) ||
						(childParams.width == LayoutParams.FILL_PARENT);
			}

			if (matchParentHeight)
			{
				measuredChildHeight = maxBottom - minTop;
				childParams.height = measuredChildHeight;
			}

			if (matchParentWidth)
			{
				measuredChildWidth = maxRight - minLeft;
				childParams.width = measuredChildWidth;
			}

			switch (mDragEdge)
			{
				case RIGHT:
					left = Math.max(r - measuredChildWidth - getPaddingRight() - l, minLeft);
					top = Math.min(getPaddingTop(), maxBottom);
					right = Math.max(r - getPaddingRight() - l, minLeft);
					bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
					break;

				case LEFT:
					left = Math.min(getPaddingLeft(), maxRight);
					top = Math.min(getPaddingTop(), maxBottom);
					right = Math.min(measuredChildWidth + getPaddingLeft(), maxRight);
					bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
					break;
			}

			child.layout(left, top, right, bottom);
		}


		initRects();

		mHorizontalDragRange = getHalfwayPivotHorizontal();

		if (mIsOpenBeforeInit)
		{
			open(false);
		}
		else
		{
			close(false);
		}

		mOnLayoutCount++;
	}

	public void open(boolean animation)
	{

		_open(animation, mRectMainOpen.left);
	}

	public void close(boolean animation)
	{
		_close(animation, mRectMainClose.left);
	}

	public void setOnSwipeListener(OnSwipeListener listener)
	{
		mOnSwipeListener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		return mDragHelper.shouldInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{

		mDragHelper.processTouchEvent(ev);
		// handle parent scroll behaviour
		if (Math.abs(mMainView.getLeft()) > mTouchSlop)
		{

			// disable parent scrolling
			ViewParent parent = getParent();
			if (parent != null)
			{
				parent.requestDisallowInterceptTouchEvent(true);
			}
		}
		else if (MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_UP || MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_CANCEL)
		{
			// enable parent scrolling
			ViewParent parent = getParent();
			if (parent != null)
			{
				parent.requestDisallowInterceptTouchEvent(false);
			}
		}
		return true;
	}

	@Override
	public void computeScroll()
	{
		if (mDragHelper != null && mDragHelper.continueSettling(true))
		{
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	private int getHalfwayPivotHorizontal()
	{
		if (mDragEdge == DragEdge.LEFT)
		{
			return mRectMainClose.left + mSecondaryView.getWidth() / 2;
		}
		else
		{
			return mRectMainClose.right - mSecondaryView.getWidth() / 2;
		}
	}

	private void initRects()
	{
		// close position of main view
		mRectMainClose.set(
				mMainView.getLeft(),
				mMainView.getTop(),
				mMainView.getRight(),
				mMainView.getBottom()
		);

		// close position of secondary view
		mRectSecClose.set(
				mSecondaryView.getLeft(),
				mSecondaryView.getTop(),
				mSecondaryView.getRight(),
				mSecondaryView.getBottom()
		);

		// open position of the main view
		mRectMainOpen.set(
				getMainOpenLeft(),
				mRectMainClose.top,
				getMainOpenLeft() + mMainView.getWidth(),
				mRectMainClose.top + mMainView.getHeight()
		);

		// open position of the secondary view
		mRectSecOpen.set(
				mRectSecClose.left,
				mRectSecClose.top,
				mRectSecClose.left + mSecondaryView.getWidth(),
				mRectSecClose.top + mSecondaryView.getHeight()
		);

	}

	private int getMainOpenLeft()
	{
		switch (mDragEdge)
		{
			case LEFT:
				return mRectMainClose.left + mSecondaryView.getWidth();

			case RIGHT:
				return mRectMainClose.left - mSecondaryView.getWidth();
			default:
				return 0;
		}
	}

	private final ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback()
	{

		private Handler mHandler = new Handler();

		@Override
		public boolean tryCaptureView(View view, int pointerId)
		{
			if (mLockDrag)
			{
				return false;
			}
			mDragHelper.captureChildView(mMainView, pointerId);
			return false;
		}

		@Override
		public int clampViewPositionHorizontal(View child, int left, int dx)
		{
			boolean enable = false;
			switch (mDragEdge)
			{
				case RIGHT:
					enable = (left < 0);
					break;
				case LEFT:
					enable = (left >= 0);
					break;
			}
			return enable ? (child.getLeft() + Math.round(1 * dx)) : child.getLeft();
		}

		@Override
		public int getViewHorizontalDragRange(View child)
		{
			return mHorizontalDragRange;
		}

		@Override
		public void onViewDragStateChanged(int state)
		{
			final State prevState = mState;
			switch (state)
			{
				case ViewDragHelper.STATE_DRAGGING:
					mState = State.DRAGGING;
					break;

				case ViewDragHelper.STATE_IDLE:
					mState = (mMainView.getLeft() == mRectMainClose.left) ? mState = State.CLOSE : State.OPEN;
					break;
			}

			if (mOnDragStateChangeListener != null && !mAborted && prevState != mState)
			{
				mOnDragStateChangeListener.onDragStateChanged(mState);
			}
		}

		@Override
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy)
		{
			ViewCompat.postInvalidateOnAnimation(SwipeableLayout.this);
		}

		@Override
		public void onViewReleased(View releasedChild, float xvel, float yvel)
		{

			final boolean velRightExceeded = pxToDp((int) xvel) >= DEFAULT_MIN_FLING_VELOCITY;
			final boolean velLeftExceeded = pxToDp((int) xvel) <= -DEFAULT_MIN_FLING_VELOCITY;

			int halfWidth = (int) (getMeasuredWidth() / 2.2);
			Log.i(TAG, "halfWidth " + halfWidth);

			final int pivotHorizontal = getHalfwayPivotHorizontal();

			switch (mDragEdge)
			{
				case RIGHT:
					int right = mMainView.getRight();
					Log.d(TAG, "mMainView.getRight() " + right);
					if (right > halfWidth) // HalfSwipe
					{
						if (velLeftExceeded || right < pivotHorizontal)
						{
							open(true);
							notifyOnHalfSwipe(true);
							return;
						}

						close(true);
						notifyOnHalfSwipe(false);
					}
					else
					{
						// FullSwipe
						handleFullSwipe(mRectMainClose.left - mRectMainClose.right);
					}
					break;
				case LEFT:
					int left = mMainView.getLeft();
					Log.d(TAG, "mMainView.getLeft() " + left);
					if (left < halfWidth) // HalfSwipe
					{
						if (velRightExceeded || left > pivotHorizontal)
						{
							open(true);
							notifyOnHalfSwipe(true);
							return;
						}
						close(true);
						notifyOnHalfSwipe(false);
					}
					else
					{
						// FullSwipe
						handleFullSwipe(mRectMainClose.left + mRectMainClose.right);
					}
					break;
			}
		}

		private void handleFullSwipe(final int finalLeft)
		{
			mDragHelper.abort();
			mDragHelper.smoothSlideViewTo(mMainView, finalLeft, mRectMainClose.top);
			ViewCompat.postInvalidateOnAnimation(SwipeableLayout.this);
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					close(true);
					notifyOnFullSwipe(true);
				}
			}, 300);
		}

		@Override
		public void onEdgeDragStarted(final int edgeFlags, final int pointerId)
		{
			super.onEdgeDragStarted(edgeFlags, pointerId);

			if (mLockDrag)
			{
				return;
			}

			boolean edgeStartLeft = (mDragEdge == DragEdge.RIGHT)
					&& edgeFlags == ViewDragHelper.EDGE_LEFT;

			boolean edgeStartRight = (mDragEdge == DragEdge.LEFT)
					&& edgeFlags == ViewDragHelper.EDGE_RIGHT;

			if (edgeStartLeft || edgeStartRight)
			{
				mDragHelper.captureChildView(mMainView, pointerId);
			}
		}
	};

	private void notifyOnHalfSwipe(boolean isOpened)
	{
		if (mOnSwipeListener != null)
		{
			mOnSwipeListener.onHalfSwipe(SwipeableLayout.this, isOpened);
		}
	}

	private void notifyOnFullSwipe(boolean isOpened)
	{

		if (mOnSwipeListener != null)
		{
			mOnSwipeListener.onFullSwipe(SwipeableLayout.this);
		}

	}

	private void _open(boolean animation, int finalLeft)
	{
		mIsOpenBeforeInit = true;
		mAborted = false;

		if (animation)
		{

			mState = State.OPENING;
			mDragHelper.smoothSlideViewTo(mMainView, finalLeft, mRectMainOpen.top);

			if (mOnDragStateChangeListener != null)
			{
				mOnDragStateChangeListener.onDragStateChanged(mState);
			}
		}
		else
		{
			mState = State.OPEN;
			mDragHelper.abort();

			mMainView.layout(
					mRectMainOpen.left,
					mRectMainOpen.top,
					mRectMainOpen.right,
					mRectMainOpen.bottom
			);

			mSecondaryView.layout(
					mRectSecOpen.left,
					mRectSecOpen.top,
					mRectSecOpen.right,
					mRectSecOpen.bottom
			);
		}

		ViewCompat.postInvalidateOnAnimation(SwipeableLayout.this);
	}

	private void _close(boolean animation, int finalLeft)
	{
		mIsOpenBeforeInit = false;
		mAborted = false;

		if (animation)
		{
			mState = State.CLOSING;
			mDragHelper.smoothSlideViewTo(mMainView, finalLeft, mRectMainClose.top);

			if (mOnDragStateChangeListener != null)
			{
				mOnDragStateChangeListener.onDragStateChanged(mState);
			}
		}
		else
		{
			mState = State.CLOSE;
			mDragHelper.abort();

			mMainView.layout(
					mRectMainClose.left,
					mRectMainClose.top,
					mRectMainClose.right,
					mRectMainClose.bottom
			);

			mSecondaryView.layout(
					mRectSecClose.left,
					mRectSecClose.top,
					mRectSecClose.right,
					mRectSecClose.bottom
			);
		}

		ViewCompat.postInvalidateOnAnimation(SwipeableLayout.this);
	}

	private int pxToDp(int px)
	{
		Resources resources = getContext().getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		return (int) (px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
	}
}
