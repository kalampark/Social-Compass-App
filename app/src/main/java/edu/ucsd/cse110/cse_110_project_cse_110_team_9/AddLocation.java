package edu.ucsd.cse110.cse_110_project_cse_110_team_9;


import android.content.Context;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class AddLocation {
    public Double latVal;
    public Double longVal;
    public String name;

    public AddLocation(String name, Double latVal,Double longVal) {
        this.name = name;
        this.latVal = latVal;
        this.longVal = longVal;
    }

    @Override
    public String toString() {
        return "AddLocation{" +
                "latVal=" + latVal +
                ", longVal=" + longVal +
                ", name='" + name + '\'' +
                '}';
    }

    public static List<AddLocation> loadJson(Context context, String path){
        try{
            InputStream input = context.getAssets().open(path);
            Reader reader = new InputStreamReader(input);
            Gson gson = new Gson();
            Type type = new TypeToken<List<AddLocation>>(){}.getType();
            return gson.fromJson(reader, type);
        }
        catch (IOException e){
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}