/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * This class is the standard implementation of <code>IAdapterManager</code>. It provides
 * fast lookup of property values with the following semantics:
 * <ul>
 * <li>At most one factory will be invoked per property lookup
 * <li>If multiple installed factories provide the same adapter, only the first found in
 * the search order will be invoked.
 * <li>The search order from a class with the definition <br>
 * <code>class X extends Y implements A, B</code><br> is as follows: <il>
 * <li>the target's class: X
 * <li>X's superclasses in order to <code>Object</code>
 * <li>a breadth-first traversal of the target class's interfaces in the order returned by
 * <code>getInterfaces</code> (in the example, A and its superinterfaces then B and its
 * superinterfaces) </il>
 * </ul>
 * 
 * @see IAdapterFactory
 * @see IAdapterManager
 */
public final class AdapterManager implements IAdapterManager, IRegistryChangeListener {
	/** 
	 * Cache of adapters for a given adaptable class. Maps String  -> Map
	 * (adaptable class name -> (adapter class name -> factory instance))
	 */
	protected HashMap adapterLookup;
	/**
	 * Cache of classes for a given type name. Avoids too many loadClass calls.
	 * (factory -> (type name -> clazz))
	 */
	protected HashMap classLookup;
	/**
	 * Cache of class lookup order. This avoids having to compute often, and
	 * provides clients with quick lookup for instanceOf checks based on type name.
	 */
	protected HashMap classSearchOrderLookup;
	/**
	 * Map of factories, keyed by <code>String</code>, fully qualified class name of
	 * the adaptable class that the factory provides adapters for. Value is a <code>List</code>
	 * of <code>IAdapterFactory</code>.
	 */
	protected final HashMap factories;

	/**
	 * Constructs a new adapter manager.
	 */
	public AdapterManager() {
		factories = new HashMap(5);
		adapterLookup = null;
		registerFactoryProxies();
		Platform.getExtensionRegistry().addRegistryChangeListener(this);
	}

	/**
	 * Given a type name, add all of the factories that respond to those types into
	 * the given table. Each entry will be keyed by the adapter class name (supplied in
	 * IAdapterFactory.getAdapterList).
	 */
	private void addFactoriesFor(String typeName, Map table) {
		List factoryList = (List) factories.get(typeName);
		if (factoryList == null)
			return;
		for (int i = 0, imax = factoryList.size(); i < imax; i++) {
			IAdapterFactory factory = (IAdapterFactory) factoryList.get(i);
			if (factory instanceof AdapterFactoryProxy) {
				String[] adapters = ((AdapterFactoryProxy) factory).getAdapterNames();
				for (int j = 0; j < adapters.length; j++) {
					if (table.get(adapters[j]) == null)
						table.put(adapters[j], factory);
				}
			} else {
				Class[] adapters = factory.getAdapterList();
				for (int j = 0; j < adapters.length; j++) {
					String adapterName = adapters[j].getName();
					if (table.get(adapterName) == null)
						table.put(adapterName, factory);
				}
			}
		}
	}

	private void cacheClassLookup(IAdapterFactory factory, Class clazz) {
		//cache reference to lookup to protect against concurrent flush
		HashMap lookup = classLookup;
		if (lookup == null) 
			classLookup = lookup = new HashMap(4);
		HashMap classes = (HashMap) lookup.get(factory);
		if (classes == null) {
			classes = new HashMap(4);
			lookup.put(factory, classes);
		}
		classes.put(clazz.getName(), clazz);
	}

	private Class cachedClassForName(IAdapterFactory factory, String typeName) {
		Class clazz = null;
		//cache reference to lookup to protect against concurrent flush
		HashMap lookup = classLookup;
		if (lookup != null) {
			HashMap classes = (HashMap) lookup.get(factory);
			if (classes != null) {
				clazz = (Class) classes.get(typeName);
			}
		}
		return clazz;
	}

	/**
	 * Returns the class with the given fully qualified name, or null
	 * if that class does not exist or belongs to a plug-in that has not
	 * yet been loaded.
	 */
	private Class classForName(IAdapterFactory factory, String typeName) {
		Class clazz = cachedClassForName(factory, typeName);
		if (clazz == null) {
			try {
				if (factory instanceof AdapterFactoryProxy)
					factory = ((AdapterFactoryProxy) factory).loadFactory(false);
				if (factory != null) {
					clazz = factory.getClass().getClassLoader().loadClass(typeName);
					cacheClassLookup(factory, clazz);
                }
			} catch (ClassNotFoundException e) {
				// class not yet loaded
			}
		}
		return clazz;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#getAdapterTypes(java.lang.Class)
	 */
	public String[] computeAdapterTypes(Class adaptable) {
		Set types = getFactories(adaptable).keySet();
		return (String[]) types.toArray(new String[types.size()]);
	}

	/**
	 * Computes the adapters that the provided class can adapt to, along
	 * with the factory object that can perform that transformation. Returns 
	 * a table of adapter class name to factory object.
	 * @param adaptable
	 */
	private Map getFactories(Class adaptable) {
		//cache reference to lookup to protect against concurrent flush
		HashMap lookup = adapterLookup;
		if (lookup == null)
			adapterLookup = lookup = new HashMap(30);
		Map table = (Map) lookup.get(adaptable.getName());
		if (table == null) {
			// calculate adapters for the class
			table = new HashMap(4);
			Class[] classes = computeClassOrder(adaptable);
			for (int i = 0; i < classes.length; i++)
				addFactoriesFor(classes[i].getName(), table);
			// cache the table
			lookup.put(adaptable.getName(), table);
		}
		return table;
	}

	public Class[] computeClassOrder(Class adaptable) {
		List classes = null;
		//cache reference to lookup to protect against concurrent flush
		HashMap lookup = classSearchOrderLookup;
		if (lookup != null)
			classes = (List) lookup.get(adaptable);
		// compute class order only if it hasn't been cached before
		if (classes == null) {
			classes = new ArrayList();
			computeClassOrder(adaptable, classes);
			if (lookup == null)
				classSearchOrderLookup =  lookup = new HashMap();
			lookup.put(adaptable, classes);
		}
		return (Class[]) classes.toArray(new Class[classes.size()]);
	}

	/**
	 * Builds and returns a table of adapters for the given adaptable type.
	 * The table is keyed by adapter class name. The
	 * value is the <b>sole<b> factory that defines that adapter. Note that
	 * if multiple adapters technically define the same property, only the
	 * first found in the search order is considered.
	 * 
	 * Note that it is important to maintain a consistent class and interface
	 * lookup order. See the class comment for more details.
	 */
	private void computeClassOrder(Class adaptable, Collection classes) {
		Class clazz = adaptable;
		Set seen = new HashSet(4);
		while (clazz != null) {
			classes.add(clazz);
			computeInterfaceOrder(clazz.getInterfaces(), classes, seen);
			clazz = clazz.getSuperclass();
		}
	}

	private void computeInterfaceOrder(Class[] interfaces, Collection classes, Set seen) {
		List newInterfaces = new ArrayList(interfaces.length);
		for (int i = 0; i < interfaces.length; i++) {
			Class interfac = interfaces[i];
			if (seen.add(interfac)) {
				//note we cannot recurse here without changing the resulting interface order
				classes.add(interfac);
				newInterfaces.add(interfac);
			}
		}
		for (Iterator it = newInterfaces.iterator(); it.hasNext();)
			computeInterfaceOrder(((Class) it.next()).getInterfaces(), classes, seen);
	}

	/**
	 * Flushes the cache of adapter search paths. This is generally required whenever an
	 * adapter is added or removed.
	 * <p>
	 * It is likely easier to just toss the whole cache rather than trying to be smart
	 * and remove only those entries affected.
	 * </p>
	 */
	public synchronized void flushLookup() {
		adapterLookup = null;
		classLookup = null;
		classSearchOrderLookup = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptable, Class adapterType) {
		IAdapterFactory factory = (IAdapterFactory) getFactories(adaptable.getClass()).get(adapterType.getName());
		Object result = null;
		if (factory != null)
			result = factory.getAdapter(adaptable, adapterType);
		if (result == null && adapterType.isInstance(adaptable))
			return adaptable;
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptable, String adapterType) {
		return getAdapter(adaptable, adapterType, false);
	}

	/**
	 * Returns an adapter of the given type for the provided adapter.
	 * @param adaptable the object to adapt
	 * @param adapterType the type to adapt the object to
	 * @param force <code>true</code> if the plug-in providing the
	 * factory should be activated if necessary. <code>false</code>
	 * if no plugin activations are desired.
	 */
	private Object getAdapter(Object adaptable, String adapterType, boolean force) {
		IAdapterFactory factory = (IAdapterFactory) getFactories(adaptable.getClass()).get(adapterType);
		if (force && factory instanceof AdapterFactoryProxy)
			factory = ((AdapterFactoryProxy) factory).loadFactory(true);
		Object result = null;
		if (factory != null) {
			Class clazz = classForName(factory, adapterType);
			if (clazz != null)
				result = factory.getAdapter(adaptable, clazz);
		}
		if (result == null && adaptable.getClass().getName().equals(adapterType))
			return adaptable;
		return result;
	}

	public boolean hasAdapter(Object adaptable, String adapterTypeName) {
		return getFactories(adaptable.getClass()).get(adapterTypeName) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#loadAdapter(java.lang.Object, java.lang.String)
	 */
	public Object loadAdapter(Object adaptable, String adapterTypeName) {
		return getAdapter(adaptable, adapterTypeName, true);
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	public synchronized void registerAdapters(IAdapterFactory factory, Class adaptable) {
		registerFactory(factory, adaptable.getName());
		flushLookup();
	}

	private void registerExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int j = 0; j < elements.length; j++) {
			AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(elements[j]);
			if (proxy != null)
				registerFactory(proxy, proxy.getAdaptableType());
		}
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	private void registerFactory(IAdapterFactory factory, String adaptableType) {
		List list = (List) factories.get(adaptableType);
		if (list == null) {
			list = new ArrayList(5);
			factories.put(adaptableType, list);
		}
		list.add(factory);
	}

	/**
	 * Loads adapters registered with the adapters extension point from
	 * the plug-in registry.  Note that the actual factory implementations
	 * are loaded lazily as they are needed.
	 */
	private void registerFactoryProxies() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(Platform.PI_RUNTIME, Platform.PT_ADAPTERS);
		if (point == null)
			return;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++)
			registerExtension(extensions[i]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public synchronized void registryChanged(IRegistryChangeEvent event) {
		//find the set of changed adapter extensions
		HashSet toRemove = null;
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		String adapterId = Platform.PI_RUNTIME + '.' + Platform.PT_ADAPTERS;
		boolean found = false;
		for (int i = 0; i < deltas.length; i++) {
			//we only care about extensions to the adapters extension point
			if (!adapterId.equals(deltas[i].getExtensionPoint().getUniqueIdentifier()))
				continue;
			found = true;
			if (deltas[i].getKind() == IExtensionDelta.ADDED)
				registerExtension(deltas[i].getExtension());
			else {
				//create the hash set lazily
				if (toRemove == null)
					toRemove = new HashSet();
				toRemove.add(deltas[i].getExtension());
			}
		}
		//need to discard cached state for the changed extensions
		if (found)
			flushLookup();
		if (toRemove == null)
			return;
		//remove any factories belonging to extensions that are going away
		for (Iterator it = factories.values().iterator(); it.hasNext();) {
			for (Iterator it2 = ((List) it.next()).iterator(); it2.hasNext();) {
				IAdapterFactory factory = (IAdapterFactory) it2.next();
				if (factory instanceof AdapterFactoryProxy) {
					IExtension ext = ((AdapterFactoryProxy) factory).getExtension();
					if (toRemove.contains(ext))
						it2.remove();
				}
			}
		}
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	public synchronized void unregisterAdapters(IAdapterFactory factory) {
		for (Iterator it = factories.values().iterator(); it.hasNext();)
			((List) it.next()).remove(factory);
		flushLookup();
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	public synchronized void unregisterAdapters(IAdapterFactory factory, Class adaptable) {
		List factoryList = (List) factories.get(adaptable.getName());
		if (factoryList == null)
			return;
		factoryList.remove(factory);
		flushLookup();
	}

	/*
	 * Shuts down the adapter manager by removing all factories
	 * and removing the registry change listener. Should only be
	 * invoked during platform shutdown.
	 */
	public synchronized void unregisterAllAdapters() {
		factories.clear();
		flushLookup();
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
	}
}
