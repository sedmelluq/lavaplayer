package com.sedmelluq.lavaplayer.core.tools.userdata;

public interface UserDataHolder {
  /**
   * Attach an object with this track which can later be retrieved with {@link #getUserData()}. Useful for retrieving
   * application-specific object from the track in callbacks.
   *
   * @param userData Object to store.
   */
  void setUserData(Object userData);

  /**
   * @return Object previously stored with {@link #setUserData(Object)}
   */
  Object getUserData();

  /**
   * @param klass The expected class of the user data (or a superclass of it).
   * @return Object previously stored with {@link #setUserData(Object)} if it is of the specified type. If it is set,
   *         but with a different type, null is returned.
   */
  <T> T getUserData(Class<T> klass);
}
