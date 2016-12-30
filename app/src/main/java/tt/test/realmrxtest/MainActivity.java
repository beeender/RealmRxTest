package tt.test.realmrxtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = MainActivity.class.getSimpleName();

    Realm realm = null;
    static int counter = 0;
    CompositeSubscription subs = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Realm.init(this.getApplicationContext());
        RealmConfiguration configuration = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(configuration);
        insertDog();

        final long now = System.currentTimeMillis();

        realm = Realm.getDefaultInstance();

        Subscription subscription = realm.where(Dog.class)
                .greaterThan("timestamp", now)
                .findAllSorted("timestamp", Sort.DESCENDING)
                .asObservable()
                .filter(new Func1<RealmResults<Dog>, Boolean>() {
                    @Override
                    public Boolean call(RealmResults<Dog> dogs) {
                        return !dogs.isEmpty();
                    }
                })
                .map(new Func1<RealmResults<Dog>, List<Dog>>() {
                    @Override
                    public List<Dog> call(RealmResults<Dog> dogs) {
                        return realm.copyFromRealm(dogs);
                    }
                })
                .subscribe(new Action1<List<Dog>>() {
                    @Override
                    public void call(List<Dog> dogs) {
                        final Dog dog = dogs.get(0);
                        Log.d(TAG, String.format(Locale.getDefault(), "#1 Got Dogs {threadId=%d,size=%d}", Thread.currentThread().getId(), dogs.size()));
                    }
                });

        Subscription subscription2 = realm.where(Dog.class)
                .greaterThan("timestamp", now)
                .findAllSorted("timestamp", Sort.DESCENDING)
                .asObservable()
                .filter(new Func1<RealmResults<Dog>, Boolean>() {
                    @Override
                    public Boolean call(RealmResults<Dog> dogs) {
                        return !dogs.isEmpty();
                    }
                })
                .map(new Func1<RealmResults<Dog>, List<Dog>>() {
                    @Override
                    public List<Dog> call(RealmResults<Dog> dogs) {
                        return realm.copyFromRealm(dogs);
                    }
                })
                .subscribe(new Action1<List<Dog>>() {
                    @Override
                    public void call(List<Dog> dogs) {
                        final Dog dog = dogs.get(0);
                        Log.d(TAG, String.format(Locale.getDefault(), "#2 Got Dogs {threadId=%d,size=%d}", Thread.currentThread().getId(), dogs.size()));
                    }
                });

        Subscription insertAsync = Observable.interval(10, TimeUnit.MILLISECONDS)
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        insertDog();
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });


        subs.add(subscription);
        subs.add(subscription2);
        subs.add(insertAsync);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subs.clear();
    }

    private void insertDog() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Dog dog = realm.createObject(Dog.class);
                dog.setName("Dog" + counter++);
                dog.setTimestamp(System.currentTimeMillis());
            }
        });
        realm.close();
    }
}
