package com.ezhang.pop.core;

import android.os.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StateMachine<EmState extends Enum<EmState>, EmEvent extends Enum<EmEvent>> {
	public interface EventAction
	{
		public void PerformAction(Bundle param);
	}
	private class Transition
	{
		public EmState nextState;
		public EmEvent event;
		public EventAction action;
		public Transition(EmState _nextState, EmEvent _event, EventAction _action)
		{
			nextState = _nextState;
			event = _event;
			action = _action;
		}
		@Override
		public boolean equals(Object obj)
		{
			if(!Transition.class.isInstance(obj))
			{
				return false;
			}
			@SuppressWarnings("unchecked")
			Transition anotherAction = (Transition)obj;
			return this.nextState == anotherAction.nextState &&
					this.event == anotherAction.event;
		}
		@Override
		public int hashCode()
		{
			return nextState.ordinal() * 1000 + event.ordinal();
		}
	}
	
	private Map<EmState, HashSet<Transition>> m_stateMap = new HashMap<EmState, HashSet<Transition>>();
	private EmState m_currentState;

	public StateMachine(EmState initState)
	{
		m_currentState = initState;
	}
	public void AddTransition(EmState curState, EmState nextState, EmEvent event, EventAction action)
	{
		if(!m_stateMap.containsKey(curState))
		{
			m_stateMap.put(curState, new HashSet<Transition>());
		}
		Set<Transition> transitions = m_stateMap.get(curState);
		transitions.add(new Transition(nextState, event, action));
		transitions.add(new Transition(nextState, event, action));
	}
	
	public void HandleEvent(EmEvent event, Bundle param)
	{
		Set<Transition> transitions = m_stateMap.get(m_currentState);
		for(Transition t : transitions)
		{
			if(t.event == event)
			{
				m_currentState = t.nextState;
				t.action.PerformAction(param);
			}
		}
	}
	public void SetState(EmState state) {
		this.m_currentState = state;
	}
	public EmState GetState() {
		return this.m_currentState;
	}
}
