package com.beyondeye.reduks.pcollections;

import java.util.Collection;
import java.util.Set;

/**
 * 
 * An immutable, persistent set, containing no duplicate elements.
 * 
 * @author harold
 *
 * @param <E>
 */
public interface PSet<E> extends PCollection<E>, Set<E> {
	//@Override
	public PSet<E> plus(E e);
	//@Override
	public PSet<E> plusAll(Collection<? extends E> list);
	//@Override
	public PSet<E> minus(Object e);
	//@Override
	public PSet<E> minusAll(Collection<?> list);
}
