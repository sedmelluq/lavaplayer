package com.sedmelluq.lavaplayer.core.tools.userdata;

public abstract class AbstractUserDataHolder implements UserDataHolder {
  private volatile Object userData;

  @Override
  public void setUserData(Object userData) {
    this.userData = userData;
  }

  @Override
  public Object getUserData() {
    return userData;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUserData(Class<T> klass) {
    Object data = userData;

    if (data != null && klass.isAssignableFrom(data.getClass())) {
      return (T) data;
    } else {
      return null;
    }
  }
}
