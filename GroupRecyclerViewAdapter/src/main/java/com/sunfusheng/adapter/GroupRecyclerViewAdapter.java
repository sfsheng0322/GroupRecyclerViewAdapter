package com.sunfusheng.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author sunfusheng on 2018/2/1.
 */
abstract public class GroupRecyclerViewAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "GroupAdapter";

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CHILD = 1;
    public static final int TYPE_FOOTER = 2;

    private Context context;
    private LayoutInflater inflater;
    private List<List<T>> items;
    private int itemPosition;
    private int itemType;

    private OnItemClickListener onItemClickListener;

    public GroupRecyclerViewAdapter(Context context) {
        this(context, new ArrayList<>());
    }

    public GroupRecyclerViewAdapter(Context context, List<List<T>> items) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.items = items;
        checkData();
    }

    public GroupRecyclerViewAdapter(Context context, T[][] items) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.items = convertData(items);
        checkData();
    }

    private void checkData() {
        if (0 == getGroupCount()) {
            return;
        }

        Iterator<List<T>> iterator = items.iterator();
        while (iterator.hasNext()) {
            List<T> item = iterator.next();
            int minCount = (showHeader() ? 1 : 0) + (showFooter() ? 1 : 0);
            if (null == item || 0 == item.size() || minCount > item.size()) {
                iterator.remove();
                Log.w(TAG, "Data illegal, already removed item " + item);
            }
        }
    }

    public List<List<T>> convertData(T[][] items) {
        List<List<T>> lists = new ArrayList<>();
        for (T[] item : items) {
            List<T> list = new ArrayList<>();
            list.addAll(Arrays.asList(item));
            lists.add(list);
        }
        return lists;
    }

    public void setItems(T[][] items) {
        setItems(convertData(items));
    }

    public void setItems(List<List<T>> items) {
        this.items = items;
        checkData();
        notifyDataSetChanged();
    }

    public List<List<T>> getItems() {
        return items;
    }

    public T getItem(int groupPosition, int childPosition) {
        return items.get(groupPosition).get(childPosition);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(getLayoutId(), parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        GroupViewHolder viewHolder = (GroupViewHolder) holder;
        int itemType = confirmItemViewType(position);
        int groupPosition = getGroupPosition(position);
        int childPosition = getGroupChildPosition(groupPosition, position);
        T item = items.get(groupPosition).get(childPosition);
        Log.d(TAG, "position: " + position + " groupPosition: " + groupPosition + " childPosition: " + childPosition);

        if (null != onItemClickListener) {
            holder.itemView.setOnClickListener(view -> {
                if (null != onItemClickListener) {
                    onItemClickListener.onItemClick(this, viewHolder, groupPosition, childPosition);
                }
            });
        }

        if (TYPE_HEADER == itemType) {
            onBindHeaderViewHolder(viewHolder, item, groupPosition);
        } else if (TYPE_CHILD == itemType) {
            onBindChildViewHolder(viewHolder, item, groupPosition, childPosition);
        } else if (TYPE_FOOTER == itemType) {
            onBindFooterViewHolder(viewHolder, item, groupPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        this.itemPosition = position;
        this.itemType = confirmItemViewType(position);
        int groupPosition = getGroupPosition(position);

        if (TYPE_HEADER == itemType) {
            return getHeaderItemType(groupPosition);
        } else if (TYPE_CHILD == itemType) {
            int childPosition = getGroupChildPosition(groupPosition, position);
            return getChildItemType(groupPosition, childPosition);
        } else if (TYPE_FOOTER == itemType) {
            return getFooterItemType(groupPosition);
        }
        return super.getItemViewType(position);
    }

    public int confirmItemViewType(int itemPosition) {
        int itemCount = 0;
        for (int i = 0, groupCount = getGroupCount(); i < groupCount; i++) {
            if (showHeader()) {
                itemCount += 1;
                if (itemPosition < itemCount) {
                    return TYPE_HEADER;
                }
            }

            itemCount += getGroupChildCount(i);
            if (itemPosition < itemCount) {
                return TYPE_CHILD;
            }

            if (showFooter()) {
                itemCount += 1;
                if (itemPosition < itemCount) {
                    return TYPE_FOOTER;
                }
            }
        }
        throw new IndexOutOfBoundsException("Confirm item type failed, " + "itemPosition = " + itemPosition + ", itemCount = " + itemCount);
    }

    public int getLayoutId() {
        int itemType = confirmItemViewType(getItemPosition());
        if (TYPE_HEADER == itemType) {
            return getHeaderLayoutId();
        } else if (TYPE_CHILD == itemType) {
            return getChildLayoutId();
        } else if (TYPE_FOOTER == itemType) {
            return getFooterLayoutId();
        }
        return 0;
    }

    /**
     * @return 返回列表下标
     */
    public int getItemPosition() {
        return itemPosition;
    }

    /**
     * @param itemPosition 列表下标
     * @return 返回所在组
     */
    public int getGroupPosition(int itemPosition) {
        int itemCount = 0;
        for (int i = 0, groupCount = getGroupCount(); i < groupCount; i++) {
            itemCount += getGroupItemCount(i);
            if (itemPosition < itemCount) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param groupPosition 组下标
     * @return 返回指定组header的列表下标，如果没有header返回-1
     */
    public int getGroupHeaderPosition(int groupPosition) {
        if (groupPosition < getGroupCount()) {
            if (!showHeader()) {
                return -1;
            }
            return getGroupItemCountRange(0, groupPosition);
        }
        return -1;
    }

    /**
     * @param groupPosition 组下标
     * @param itemPosition  列表下标
     * @return 返回所在组的下标
     */
    public int getGroupChildPosition(int groupPosition, int itemPosition) {
        if (groupPosition < getGroupCount()) {
            int position = itemPosition - getGroupItemCountRange(0, groupPosition);
            if (0 <= position) {
                return position;
            }
        }
        return -1;
    }

    /**
     * @param groupPosition 组下标
     * @return 返回指定组footer的列表下标，如果没有footer返回-1
     */
    public int getGroupFooterPosition(int groupPosition) {
        if (groupPosition < getGroupCount()) {
            if (!showFooter()) {
                return -1;
            }
            return getGroupItemCountRange(0, groupPosition + 1) - 1;
        }
        return -1;
    }

    /**
     * @return 返回所有组所有项的个数
     */
    @Override
    public int getItemCount() {
        return getGroupItemCountRange(0, getGroupCount());
    }

    /**
     * @return 返回所有组的个数
     */
    public int getGroupCount() {
        return null == items ? 0 : items.size();
    }

    /**
     * @param groupPosition 组下标
     * @return 返回指定组所有项的个数，包括header、child、footer
     */
    public int getGroupItemCount(int groupPosition) {
        if (0 == getGroupCount()) {
            return 0;
        }

        return null == items.get(groupPosition) ? 0 : items.get(groupPosition).size();
    }

    /**
     * @param groupPosition 组下标
     * @return 返回指定组所有child项的个数，只含有child，不包括header、footer
     */
    public int getGroupChildCount(int groupPosition) {
        int groupItemCount = getGroupItemCount(groupPosition);
        if (0 == groupItemCount) {
            return 0;
        }

        int childCount = groupItemCount - (showHeader() ? 1 : 0) - (showFooter() ? 1 : 0);
        if (0 > childCount) {
            return 0;
        }
        return childCount;
    }

    /**
     * @param start 开始的组
     * @param count 组的个数
     * @return 返回多个组的所有项
     */
    public int getGroupItemCountRange(int start, int count) {
        int itemCount = 0;
        for (int i = start, groupCount = getGroupCount(); i < start + count && i < groupCount; i++) {
            itemCount += getGroupItemCount(i);
        }
        return itemCount;
    }

    public int getHeaderItemType(int groupPosition) {
        return TYPE_HEADER;
    }

    public int getChildItemType(int groupPosition, int childPosition) {
        return TYPE_CHILD;
    }

    public int getFooterItemType(int groupPosition) {
        return TYPE_FOOTER;
    }

    public boolean showHeader() {
        return false;
    }

    public boolean showFooter() {
        return false;
    }

    abstract public int getHeaderLayoutId();

    abstract public int getChildLayoutId();

    abstract public int getFooterLayoutId();

    abstract public void onBindHeaderViewHolder(GroupViewHolder holder, T item, int groupPosition);

    abstract public void onBindChildViewHolder(GroupViewHolder holder, T item, int groupPosition, int childPosition);

    abstract public void onBindFooterViewHolder(GroupViewHolder holder, T item, int groupPosition);

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(GroupRecyclerViewAdapter adapter, GroupViewHolder holder, int groupPosition, int childPosition);
    }

}