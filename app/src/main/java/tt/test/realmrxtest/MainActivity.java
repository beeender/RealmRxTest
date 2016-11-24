package tt.test.realmrxtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

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

        realm = Realm.getDefaultInstance();

        Subscription subscription = realm.where(Dog.class).findAll().asObservable()
                .subscribe(new Action1<RealmResults<Dog>>() {
                    @Override
                    public void call(RealmResults<Dog> dogs) {
                        Dog dog = realm.copyFromRealm(dogs.first());
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
            }
        });
        realm.close();
    }
}
