/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package math.geom3d;

/**
 *
 * @author peter
 * @param <T>
 */
public interface ShapeSet3D<T extends Shape3D> extends Shape3D, Iterable<T> {

	/**
	 * Appends the specified shape to the end of this set (optional operation).
	 */
	public boolean add(T shape);
	
	/**
	 * Inserts the specified shape at the specified position in this set 
	 * (optional operation).
	 */
	public void add(int index, T shape);
	
	/**
	 * Returns the shape at a given position.
	 * @param index the position of the shape
	 * @return the shape at the given position
	 */
	public T get(int index);
	
	/**
	 * Removes the first occurrence of the specified element from this list, 
	 * if it is present. If the list does not contain the element, it is
	 * unchanged. 
	 * Returns true if this list contained the specified element 
	 * (or equivalently, if this list changed as a result of the call). 
	 */
	public boolean remove(T shape);
	
	/**
	 * Removes the shape at the specified position in this set (optional
	 * operation).
	 */
	public T remove(int index);
	
	/** 
	 * Returns true if this set contains the shape.
	 */
	public boolean contains(T shape);

	/**
	 * Returns the index of the shape in this set. 
	 */
	public int indexOf(T shape);
	
	/**
	 * Returns the number of shapes stored in this set.
	 */
	public int size();

	/**
	 * Removes all the shapes stored in this set.
	 */
	public void clear();
}
