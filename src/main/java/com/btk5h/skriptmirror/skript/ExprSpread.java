package com.btk5h.skriptmirror.skript;

import com.btk5h.skriptmirror.Util;

import org.bukkit.event.Event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;

public class ExprSpread<T> implements Expression<T> {
  static {
    //noinspection unchecked
    Skript.registerExpression(ExprSpread.class, Object.class, ExpressionType.COMBINED,
        "...%object%");
  }

  private Expression<Object> object;

  private final ExprSpread<?> source;
  private final Class<? extends T>[] types;
  private final Class<T> superType;

  @SuppressWarnings("unchecked")
  public ExprSpread() {
    this(null, (Class<? extends T>) Object.class);
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  private ExprSpread(ExprSpread<?> source, Class<? extends T>... types) {
    this.source = source;

    if (source != null) {
      this.object = source.object;
    }

    this.types = types;
    this.superType = (Class<T>) Utils.getSuperType(types);
  }

  @Override
  public T getSingle(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T[] getArray(Event e) {
    return getAll(e);
  }

  @Override
  public T[] getAll(Event e) {
    Object obj = object.getSingle(e);

    if (obj instanceof Collection) {
      obj = ((Collection) obj).toArray();
    } else if (obj instanceof Iterable) {
      obj = toArray(((Iterable) obj).iterator());
    } else if (obj instanceof Iterator) {
      obj = toArray((Iterator<?>) obj);
    }

    if (obj == null || !obj.getClass().isArray()) {
      return Util.newArray(superType, 0);
    }

    Class<?> componentType = obj.getClass().getComponentType();
    if (componentType != null && componentType.isPrimitive()) {
      int length = Array.getLength(obj);
      Object[] boxedArray = Util.newArray(Util.WRAPPER_CLASSES.get(componentType), length);

      for (int i = 0; i < length; i++) {
        boxedArray[i] = Array.get(obj, i);
      }

      obj = boxedArray;
    }

    return Converters.convertArray((Object[]) obj, types, superType);
  }

  private Object[] toArray(Iterator<?> iter) {
    List<Object> list = new ArrayList<>();
    iter.forEachRemaining(list::add);
    return list.toArray();
  }

  @Override
  public boolean isSingle() {
    return false;
  }

  @Override
  public boolean check(Event e, Checker<? super T> c, boolean negated) {
    return SimpleExpression.check(getAll(e), c, negated, getAnd());
  }

  @Override
  public boolean check(Event e, Checker<? super T> c) {
    return SimpleExpression.check(getAll(e), c, false, getAnd());
  }

  @Override
  public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
    return new ExprSpread<>(this, to);
  }

  @Override
  public Class<? extends T> getReturnType() {
    return superType;
  }

  @Override
  public boolean getAnd() {
    return true;
  }

  @Override
  public boolean setTime(int time) {
    return false;
  }

  @Override
  public int getTime() {
    return 0;
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Override
  public Iterator<? extends T> iterator(Event e) {
    return new ArrayIterator<>(getAll(e));
  }

  @Override
  public boolean isLoopOf(String s) {
    return false;
  }

  @Override
  public Expression<?> getSource() {
    return source == null ? this : source;
  }

  @Override
  public Expression<? extends T> simplify() {
    return this;
  }

  @Override
  public Class<?>[] acceptChange(Changer.ChangeMode mode) {
    return null;
  }

  @Override
  public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(Event e, boolean debug) {
    return "spread " + object.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    object = (Expression<Object>) exprs[0];
    return !(object instanceof UnparsedLiteral);
  }
}