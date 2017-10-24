package io.github.xtonousou.soundboardx;

class Sound {

    private String name;
    private final int resourceId;
    private boolean favorite;

    public Sound(String name, int resourceId) {
        this.name = name;
        this.resourceId = resourceId;
        favorite = SharedPrefs.getInstance().isSoundFavorited(name);
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
        SharedPrefs.getInstance().setSoundFavorited(name, favorite);
    }

    @Override
    public String toString() {
        return name;
    }
}
