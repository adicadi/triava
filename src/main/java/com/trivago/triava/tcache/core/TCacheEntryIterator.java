package com.trivago.triava.tcache.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;

import javax.cache.Cache;
import javax.cache.Cache.Entry;

import com.trivago.triava.tcache.eviction.Cache.AccessTimeObjectHolder;
import com.trivago.triava.tcache.statistics.StatisticsCalculator;

/**
 * An Iterator for Cache Entries.
 * If {@link #remove()} is called it has the same effects, as calling cache.remove(key). This includes effects on statistics and the CacheListener REMOVE notification.
 * 
 * @author cesken
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class TCacheEntryIterator<K, V> implements Iterator<Entry<K,V>>
{
	private final Iterator<java.util.Map.Entry<K, AccessTimeObjectHolder<V>>> mapIterator;
	Entry<K, V> currentElement = null;
	private final Cache<K,V> cache;
	private final com.trivago.triava.tcache.eviction.Cache<K,V> tcache;
	StatisticsCalculator statisticsCalculator;

	public TCacheEntryIterator(com.trivago.triava.tcache.eviction.Cache<K, V> tcache, ConcurrentMap<K, AccessTimeObjectHolder<V>> objects)
	{
		mapIterator = objects.entrySet().iterator();
		List<javax.cache.Cache.Entry<K,V>> entries = new ArrayList<>();
		for (java.util.Map.Entry<K, AccessTimeObjectHolder<V>> entry : objects.entrySet())
		{
			entries.add(new TCacheJSR107Entry<K, V>(entry.getKey(), entry.getValue().peek()));
		}

		this.tcache = tcache;
		this.statisticsCalculator = tcache.statisticsCalculator();
		this.cache = tcache.jsr107cache();
	}

	@Override
	public boolean hasNext()
	{
		return mapIterator.hasNext();
	}

	@Override
	public Entry<K, V> next()
	{
		try
		{
			java.util.Map.Entry<K, AccessTimeObjectHolder<V>> entry = mapIterator.next();
			currentElement = new TCacheJSR107Entry<K, V>(entry.getKey(), entry.getValue().peek());
			if (statisticsCalculator != null)
				statisticsCalculator.incrementHitCount();
			return currentElement;
			
		}
		catch (NoSuchElementException nsee)
		{
			currentElement = null;
			throw nsee;
		}
	}
	
	@Override
	public void remove()
	{
		if (currentElement == null)
			throw new IllegalStateException("No element to remove");
		cache.remove(currentElement.getKey());
		// Done: Actions like statistics and write-through are done by cache.remove()
	}
}
