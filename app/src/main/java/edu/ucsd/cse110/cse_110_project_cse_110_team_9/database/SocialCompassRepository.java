package edu.ucsd.cse110.cse_110_project_cse_110_team_9.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class SocialCompassRepository {
    private final SocialCompassDao dao;


    public SocialCompassRepository(SocialCompassDao dao) {
        this.dao = dao;

    }


    public LiveData<Friend> getFriendFromRemoteLive(String public_uid)
    {
        return ServerAPI.provide().getFriendUpdates(public_uid);
    }

    public Friend getFriendFromRemote(String public_uid)
    {
        var future = ServerAPI.provide().getFriendAsync(public_uid);

        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        } catch (TimeoutException e) {
        }

        return null;
    }

    public LiveData<List<Friend>> getAllLocalFriendsLive() {
        return dao.getAllFriendsLive();
    }
    public List<Friend> getAllLocalFriends() {
        return dao.getAllFriends();
    }


    public void shutdownPool()
    {
        ServerAPI.provide().shutDownPool();
    }

    public void restartThreadPool()
    {
        ServerAPI.provide().restartThreadPool();
    }

    public boolean friendExistsLocal(String public_uid) {
        return dao.friendExists(public_uid);
    }


    public Friend[] getAllPubliclyListedFriends()
    {

        var fur = ServerAPI.provide().getallPublicListedLocationsAsync();

        try {
            var friends = fur.get(1, TimeUnit.SECONDS);
            return  friends;
        } catch (ExecutionException e) {
            //
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        } catch (TimeoutException e) {
            //throw new RuntimeException(e);
        }
        return null;
    }

//    public Friend getFriendFromRemote(String public_uid)
//    {
//        ServerAPI.provide().getFriendAsync();
//    }

    public boolean friendExistsRemote(String public_uid)
    {
        var fur = ServerAPI.provide().getFriendAsync(public_uid);
        try {
            var friend = fur.get(1, TimeUnit.SECONDS);
            if (friend != null)
            {
                return  true;
            }
        } catch (ExecutionException e) {
            //
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        } catch (TimeoutException e) {
            //throw new RuntimeException(e);
        }
        return false;
    }

    public void upsertUserRemote(User user) {
        ServerAPI.provide().updateUserLocationAsync(user);
    }

    public boolean userExists() {
        return dao.userExists();
    }
    public LiveData<User> getLiveUser() {
        return dao.getLiveUser();
    }

    public User getUser() {
        return dao.getUser();
    }

    public void deleteLocalFriend(Friend friend) {
        dao.deleteFriend(friend);
    }

    public void deleteUserOnRemoteLocally(User user)
    {
        ServerAPI.provide().deleteUserLocationOnRemoteAsync(user);
        dao.deleteUser(user);

    }


    public void upsertLocalUser(User user) {
        dao.upsertUser(user);
    }


    public void nukeFriends()
    {
        dao.nukeFriends();
    }

    public void nukeUser()
    {
        dao.nukeUser();
    }


    public void upsertLocalFriend(String public_uid) {
        var fur = ServerAPI.provide().getFriendAsync(public_uid);
        try {
            var friend = fur.get(1, TimeUnit.SECONDS);
            dao.upsertFriend(friend);
        } catch (ExecutionException e) {
            //throw new RuntimeException(e);
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        } catch (TimeoutException e) {
           // throw new RuntimeException(e);
        }


    }


}
