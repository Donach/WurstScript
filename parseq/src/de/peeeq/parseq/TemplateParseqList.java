package de.peeeq.parseq;

public class TemplateParseqList {

	public static void writeTo(StringBuilder sb) {
		sb.append("import java.util.Collection;\n");
		sb.append("import java.util.Iterator;\n");
		sb.append("import java.util.ArrayList;\n");
		sb.append("import java.util.List;\n");
		sb.append("import java.util.ListIterator;\n");
		sb.append("\n");
		sb.append("abstract class ParseqList<T> implements List<T> {\n");
		sb.append("	\n");
		sb.append("	private List<T> list = new ArrayList<T>();\n");
		sb.append("	\n");
		sb.append("	abstract protected void other_setParentToThis(T t);\n");
		sb.append("	abstract protected void other_clearParent(T t);\n");
		sb.append("	\n");
		sb.append("	public boolean add(T t) {\n");
		sb.append("		other_setParentToThis(t);\n");
		sb.append("		return list.add(t);\n");
		sb.append("	}\n");
		sb.append("\n");
		
		sb.append("	public void addFront(T t) { add(0, t); }\n");
		
		sb.append("	public List<T> removeAll() {\n");
		sb.append("		List<T> result = new ArrayList<T>();\n");
		sb.append("		while (!isEmpty()) {\n");
		sb.append("			result.add(remove(0));\n");
		sb.append("		}\n");
		sb.append("		return result;\n");
		sb.append("	}\n");
		
		sb.append("	@Override\n");
		sb.append("	public void add(int index, T elem) {\n");
		sb.append("		other_setParentToThis(elem);\n");
		sb.append("		list.add(index, elem);		\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean addAll(Collection<? extends T> c) {\n");
		sb.append("		for (T t: c) {\n");
		sb.append("			other_setParentToThis(t);\n");
		sb.append("		}\n");
		sb.append("		return list.addAll(c);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean addAll(int pos, Collection<? extends T> c) {\n");
		sb.append("		for (T t: c) {\n");
		sb.append("			other_setParentToThis(t);\n");
		sb.append("		}\n");
		sb.append("		return list.addAll(pos, c);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public void clear() {\n");
		sb.append("		for (T t : list) {\n");
		sb.append("			other_clearParent(t);\n");
		sb.append("		}\n");
		sb.append("		list.clear();\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean contains(Object arg0) {\n");
		sb.append("		return list.contains(arg0);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean containsAll(Collection<?> c) {\n");
		sb.append("		return list.containsAll(c);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public T get(int index) {\n");
		sb.append("		return list.get(index);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public int indexOf(Object o) {\n");
		sb.append("		return list.indexOf(o);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean isEmpty() {\n");
		sb.append("		return list.isEmpty();\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public Iterator<T> iterator() {\n");
		sb.append("		return list.iterator();\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public int lastIndexOf(Object o) {\n");
		sb.append("		return list.lastIndexOf(list);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public ListIterator<T> listIterator(int index) {\n");
		sb.append("		return new ParseqListIterator(list.listIterator(index));\n");
		sb.append("	}\n");
		sb.append("	@Override\n");
		sb.append("	public ListIterator<T> listIterator() {\n");
		sb.append("		return new ParseqListIterator(list.listIterator());\n");
		sb.append("	}\n");
		sb.append("	\n");
		sb.append("	class ParseqListIterator implements ListIterator<T> {\n");
		sb.append("\n");
		sb.append("		private ListIterator<T> it;\n");
		sb.append("		private T lastElement;\n");
		sb.append("\n");
		sb.append("		public ParseqListIterator(ListIterator<T> listIterator) {\n");
		sb.append("			this.it = listIterator;\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public void add(T e) {\n");
		sb.append("			other_setParentToThis(e);\n");
		sb.append("			it.add(e);\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public boolean hasNext() {\n");
		sb.append("			return it.hasNext();\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public boolean hasPrevious() {\n");
		sb.append("			return it.hasPrevious();\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public T next() {\n");
		sb.append("			lastElement = it.next();\n");
		sb.append("			return lastElement;\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public int nextIndex() {\n");
		sb.append("			return it.nextIndex();\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public T previous() {\n");
		sb.append("			lastElement = it.previous();\n");
		sb.append("			return lastElement;\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public int previousIndex() {\n");
		sb.append("			return it.previousIndex();\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public void remove() {\n");
		sb.append("			if (lastElement == null) throw new Error();\n");
		sb.append("			other_clearParent(lastElement);\n");
		sb.append("			lastElement = null;\n");
		sb.append("			it.remove();\n");
		sb.append("		}\n");
		sb.append("\n");
		sb.append("		@Override\n");
		sb.append("		public void set(T e) {\n");
		sb.append("			if (lastElement == null) throw new Error();\n");
		sb.append("			other_clearParent(lastElement);\n");
		sb.append("			lastElement = null;\n");
		sb.append("			other_setParentToThis(e);\n");
		sb.append("			it.set(e);\n");
		sb.append("		}\n");
		sb.append("		\n");
		sb.append("	}\n");
		sb.append("\n");

		sb.append(" @SuppressWarnings(\"unchecked\")\n");
		sb.append("	@Override\n");
		sb.append("	public boolean remove(Object o) {\n");
		sb.append("		if (list.remove(o)) {\n");
		sb.append("			other_clearParent((T) o);\n");
		sb.append("			return true;\n");
		sb.append("		}\n");
		sb.append("		return false;\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public T remove(int index) {\n");
		sb.append("		T t = list.remove(index);\n");
		sb.append("		other_clearParent(t);\n");
		sb.append("		return t;\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean removeAll(Collection<?> c) {\n");
		sb.append("		boolean changed = false;\n");
		sb.append("		for (Object o : c) {\n");
		sb.append("			changed = remove(o) || changed; // order important here\n");
		sb.append("		}\n");
		sb.append("		return true;\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public boolean retainAll(Collection<?> c) {\n");
		sb.append("		ListIterator<T> li = list.listIterator();\n");
		sb.append("		boolean changed = false;\n");
		sb.append("		while (li.hasNext()) {\n");
		sb.append("			T t = li.next();\n");
		sb.append("			if (!c.contains(t)) {\n");
		sb.append("				li.remove();\n");
		sb.append("				other_clearParent(t);\n");
		sb.append("				changed  = true;\n");
		sb.append("			}\n");
		sb.append("		}\n");
		sb.append("		return changed;\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public T set(int index, T element) {\n");
		sb.append("		other_setParentToThis(element);\n");
		sb.append("		T t = list.set(index, element);		\n");
		sb.append("		other_clearParent(t);\n");
		sb.append("		return t;\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public int size() {\n");
		sb.append("		return list.size();\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public List<T> subList(int fromIndex, int toIndex) {\n");
		sb.append("		return list.subList(fromIndex, toIndex);\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public Object[] toArray() {\n");
		sb.append("		return list.toArray();\n");
		sb.append("	}\n");
		sb.append("\n");
		sb.append("	@Override\n");
		sb.append("	public <S> S[] toArray(S[] a) {\n");
		sb.append("		return list.toArray(a);\n");
		sb.append("	}\n");
		sb.append("	\n");
		sb.append("	\n");
		sb.append("	\n");
		sb.append("}\n");

		
	}

}
