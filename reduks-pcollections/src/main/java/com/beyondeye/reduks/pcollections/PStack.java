/*
 * Copyright (c) 2008 Harold Cooper. All rights reserved.
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

package com.beyondeye.reduks.pcollections;

import java.util.Collection;

/**
 * An immutable, persistent stack.
 *
 * @author harold
 * @param <E>
 */
public interface PStack<E> extends PSequence<E> {

  /** Returns a stack consisting of the elements of this with e prepended. */
  // @Override
  public PStack<E> plus(E e);

  /** Returns a stack consisting of the elements of this with list prepended in reverse. */
  // @Override
  public PStack<E> plusAll(Collection<? extends E> list);

  // @Override
  public PStack<E> with(int i, E e);

  // @Override
  public PStack<E> plus(int i, E e);

  // @Override
  public PStack<E> plusAll(int i, Collection<? extends E> list);

  // @Override
  public PStack<E> minus(Object e);

  // @Override
  public PStack<E> minusAll(Collection<?> list);

  // @Override
  public PStack<E> minus(int i);

  // @Override
  public PStack<E> subList(int start, int end);

  /**
   * @param start
   * @return subList(start,this.size())
   */
  public PStack<E> subList(int start);
}
