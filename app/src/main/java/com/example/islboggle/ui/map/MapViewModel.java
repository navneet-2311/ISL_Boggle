package com.example.islboggle.ui.map;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.islboggle.data.Level;
import com.example.islboggle.data.LevelRepository;
import java.util.List;

public class MapViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Level>> levelsData;

    public MapViewModel(@NonNull Application application) {
        super(application);
        LevelRepository repository = new LevelRepository(application);
        levelsData = new MutableLiveData<>(repository.getLevels());
    }

    public LiveData<List<Level>> getLevels() {
        return levelsData;
    }

    public void refreshLevels() {
        LevelRepository repository = new LevelRepository(getApplication());
        levelsData.setValue(repository.getLevels());
    }
}
