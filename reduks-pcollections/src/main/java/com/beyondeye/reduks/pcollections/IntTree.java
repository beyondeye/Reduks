package com.beyondeye.reduks.pcollections;

import java.util.Iterator;
import java.util.Map.Entry;




/**
 * 
 * A non-public utility class for persistent balanced tree maps with integer keys.
 * <p>
 * To allow for efficiently increasing all keys above a certain value or decreasing
 * all keys below a certain value, the keys values are stored relative to their parent.
 * This makes this map a good backing for fast insertion and removal of indices in a
 * vector.
 * <p>
 * This implementation is thread-safe except for its iterators.
 * <p>
 * Other than that, this tree is based on the Glasgow Haskell Compiler's Data.Map implementation,
 * which in turn is based on "size balanced binary trees" as described by:
 * <p>
 * Stephen Adams, "Efficient sets: a balancing act",
 * Journal of Functional Programming 3(4):553-562, October 1993,
 * http://www.swiss.ai.mit.edu/~adams/BB/.
 * <p>
 * J. Nievergelt and E.M. Reingold, "Binary search trees of bounded balance",
 * SIAM journal of computing 2(1), March 1973.
 * 
 * @author harold
 *
 * @param <V>
 */
class IntTree<V> {
	// marker value:
	static final IntTree<Object> EMPTYNODE = new IntTree<Object>();
	
	private final long key; // we use longs so relative keys can express all ints
		// (e.g. if this has key -10 and right has 'absolute' key MAXINT,
		// then its relative key is MAXINT+10 which overflows)
		// there might be some way to deal with this based on left-verse-right logic,
		// but that sounds like a mess.
	private final V value; // null value means this is empty node
	private final IntTree<V> left, right;
	private final int size;
	private IntTree() {
		if(EMPTYNODE!=null)
			throw new RuntimeException("empty constructor should only be used once");
		size = 0;
		
		key=0; value=null; left=null; right=null;
	}
	private IntTree(final long key, final V value, final IntTree<V> left, final IntTree<V> right) {
		this.key = key; this.value = value;
		this.left = left; this.right = right;
		size = 1 + left.size + right.size;
	}
	
	private IntTree<V> withKey(final long newKey) {
		if(size==0 || newKey==key) return this;
		return new IntTree<V>(newKey, value, left, right); }
	
	Iterator<Entry<Integer,V>> iterator() {
		return new EntryIterator<V>(this); }
	
	int size() {
		return size; }

	boolean containsKey(final long key) {
		if(size==0)
			return false;
		if(key < this.key)
			return left.containsKey(key-this.key);
		if(key > this.key)
			return right.containsKey(key-this.key);
		// otherwise key==this.key:
		return true;
	}
	
	V get(final long key) {
		if(size==0)
			return null;
		if(key < this.key)
			return left.get(key-this.key);
		if(key > this.key)
			return right.get(key-this.key);
		// otherwise key==this.key:
		return value;
	}

	IntTree<V> plus(final long key, final V value) {
		if(size==0)
			return new IntTree<V>(key, value, this, this);
		if(key < this.key)
			return rebalanced(left.plus(key-this.key, value), right);
		if(key > this.key)
			return rebalanced(left, right.plus(key-this.key, value));
		// otherwise key==this.key, so we simply replace this, with no effect on balance:
		if(value==this.value)
			return this;
		return new IntTree<V>(key, value, left, right);
	}

	IntTree<V> minus(final long key) {
		if(size==0)
			return this;
		if(key < this.key)
			return rebalanced(left.minus(key-this.key), right);
		if(key > this.key)
			return rebalanced(left, right.minus(key-this.key));

		// otherwise key==this.key, so we are killing this node:

		if(left.size==0) // we can just become right node
			// make key 'absolute':
			return right.withKey(right.key+this.key);
		if(right.size==0) // we can just become left node
			return left.withKey(left.key+this.key);

		// otherwise replace this with the next key (i.e. the smallest key to the right):
		
		// TODO have minNode() instead of minKey to avoid having to call get()
		// TODO get node from larger subtree, i.e. if left.size>right.size use left.maxNode()
		// TODO have faster minusMin() instead of just using minus()
		
		long newKey = right.minKey() + this.key;
			//(right.minKey() is relative to this; adding this.key makes it 'absolute'
			//	where 'absolute' really means relative to the parent of this)

		V newValue = right.get(newKey-this.key);
		// now that we've got the new stuff, take it out of the right subtree:
		IntTree<V> newRight = right.minus(newKey-this.key);

		// lastly, make the subtree keys relative to newKey (currently they are relative to this.key):
		newRight = newRight.withKey( (newRight.key+this.key) - newKey );
		// left is definitely not empty:
		IntTree<V> newLeft = left.withKey( (left.key+this.key) - newKey );
		
		return rebalanced(newKey, newValue, newLeft, newRight);
	}
	
	/**
	 * Changes every key k>=key to k+delta.
	 * 
	 * This method will create an _invalid_ tree if delta<0
	 * and the distance between the smallest k>=key in this
	 * and the largest j<key in this is |delta| or less.
	 * 
	 * In other words, this method must not result in any change
	 * in the order of the keys in this, since the tree structure is
	 * not being changed at all.
	 */
	IntTree<V> changeKeysAbove(final long key, final int delta) {
		if(size==0 || delta==0)
			return this;

		if(this.key>=key)
			// adding delta to this.key changes the keys of _all_ children of this,
			// so we now need to un-change the children of this smaller than key,
			// all of which are to the left. note that we still use the 'old' relative key...:
			return new IntTree<V>(this.key+delta, value, left.changeKeysBelow(key-this.key, -delta), right);

		// otherwise, doesn't apply yet, look to the right:
		IntTree<V> newRight = right.changeKeysAbove(key-this.key, delta);
		if(newRight==right) return this;
		return new IntTree<V>(this.key, value, left, newRight);
	}
	
	/**
	 * Changes every key k<key to k+delta.
	 * 
	 * This method will create an _invalid_ tree if delta>0
	 * and the distance between the largest k<key in this
	 * and the smallest j>=key in this is delta or less.
	 * 
	 * In other words, this method must not result in any overlap or change
	 * in the order of the keys in this, since the tree _structure_ is
	 * not being changed at all.
	 */
	IntTree<V> changeKeysBelow(final long key, final int delta) {
		if(size==0 || delta==0)
			return this;

		if(this.key<key)
			// adding delta to this.key changes the keys of _all_ children of this,
			// so we now need to un-change the children of this larger than key,
			// all of which are to the right. note that we still use the 'old' relative key...:
			return new IntTree<V>(this.key+delta, value, left, right.changeKeysAbove(key-this.key, -delta));

		// otherwise, doesn't apply yet, look to the left:
		IntTree<V> newLeft = left.changeKeysBelow(key-this.key, delta);
		if(newLeft==left) return this;
		return new IntTree<V>(this.key, value, newLeft, right);
	}
	
	// min key in this:
	private long minKey() {
		if(left.size==0)
			return key;
		// make key 'absolute' (i.e. relative to the parent of this):
		return left.minKey() + this.key;
	}

	private IntTree<V> rebalanced(final IntTree<V> newLeft, final IntTree<V> newRight) {
		if(newLeft==left && newRight==right)
			return this; // already balanced
		return rebalanced(key, value, newLeft, newRight);
	}

	private static final int OMEGA = 5;
	private static final int ALPHA = 2;
	// rebalance a tree that is off-balance by at most 1:
	private static <V> IntTree<V> rebalanced(final long key, final V value,
			final IntTree<V> left, final IntTree<V> right) {
		if(left.size + right.size > 1) {
			if(left.size >= OMEGA*right.size) { // rotate to the right
				IntTree<V> ll = left.left, lr = left.right;
				if(lr.size < ALPHA*ll.size) // single rotation
					return new IntTree<V>(left.key+key, left.value,
							ll,
							new IntTree<V>(-left.key, value,
									lr.withKey(lr.key+left.key),
									right));
				else { // double rotation:
					IntTree<V> lrl = lr.left, lrr = lr.right;
					return new IntTree<V>(lr.key+left.key+key, lr.value,
							new IntTree<V>(-lr.key, left.value,
									ll,
									lrl.withKey(lrl.key+lr.key)),
							new IntTree<V>(-left.key-lr.key, value,
									lrr.withKey(lrr.key+lr.key+left.key),
									right));
				}
			}
			else if(right.size >= OMEGA*left.size) { // rotate to the left
				IntTree<V> rl = right.left, rr = right.right;
				if(rl.size < ALPHA*rr.size) // single rotation
					return new IntTree<V>(right.key+key, right.value,
							new IntTree<V>(-right.key, value,
									left,
									rl.withKey(rl.key+right.key)),
							rr);
				else { // double rotation:
					IntTree<V> rll = rl.left, rlr = rl.right;
					return new IntTree<V>(rl.key+right.key+key, rl.value,
							new IntTree<V>(-right.key-rl.key, value,
									left,
									rll.withKey(rll.key+rl.key+right.key)),
							new IntTree<V>(-rl.key, right.value,
									rlr.withKey(rlr.key+rl.key),
									rr));
				}
			}
		}
		// otherwise already balanced enough:
		return new IntTree<V>(key, value, left, right);
	}

	
////entrySet().iterator() IMPLEMENTATION ////	
	// TODO make this a ListIterator?
	private static final class EntryIterator<V> implements Iterator<Entry<Integer,V>> {
		private PStack<IntTree<V>> stack = ConsPStack.empty(); //path of nonempty nodes
		private int key = 0; // note we use _int_ here since this is a truly absolute key
		
		EntryIterator(final IntTree<V> root) {
			gotoMinOf(root); }
		
		public boolean hasNext() {
			return stack.size()>0; }
		
		public Entry<Integer,V> next() {
			IntTree<V> node = stack.get(0);
			final Entry<Integer,V> result = new SimpleImmutableEntry<Integer,V>(key, node.value);
			
			// find next node.
			// we've already done everything smaller,
			// so try least larger node:
			
			if(node.right.size>0) // we can descend to the right
				gotoMinOf(node.right);
			
			else // can't descend to the right -- try ascending to the right
				while (true) { // find current node's least larger ancestor, if any
					key -= node.key; // revert to parent's key
					stack = stack.subList(1); // climb up to parent
					// if parent was larger than child or there was no parent, we're done:
					if(node.key<0 || stack.size()==0)
						break;
					// otherwise parent was smaller -- try its parent:
					node = stack.get(0);
				}
			
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException(); }

		// extend the stack to its least non-empty node:
		private void gotoMinOf(IntTree<V> node) {
			while(node.size>0) {
				stack = stack.plus(node);
				key += node.key;
				node = node.left;
			}
		}
	}
}
