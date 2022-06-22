package com.example.AnythingGroup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

public abstract class LoadWorker extends Worker {

    public LoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data.Builder error = new Data.Builder();

        Data input = this.getInputData();

        try {
            Result result = this.Work(input);

            if (result == null){
                return Result.success();
            }
            else{
                return result;
            }
        }
        catch (org.jsoup.HttpStatusException e){
            if (e.getStatusCode() == 404){
                error.putInt("error_code", e.getStatusCode());
            }
            else{
                error.putString("error", "Что-то пошло не так");
            }
        }
        catch (IOException e) {
            error.putString("error", "Проблемы с соединением");
        }

        return Result.failure(error.build());
    }

    public abstract Result Work(Data input) throws IOException;
}
