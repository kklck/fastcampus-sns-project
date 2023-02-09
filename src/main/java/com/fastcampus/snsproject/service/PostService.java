package com.fastcampus.snsproject.service;

import com.fastcampus.snsproject.exception.ErrorCode;
import com.fastcampus.snsproject.exception.SnsApplicationException;
import com.fastcampus.snsproject.model.AlarmArgs;
import com.fastcampus.snsproject.model.AlarmType;
import com.fastcampus.snsproject.model.Comment;
import com.fastcampus.snsproject.model.Post;
import com.fastcampus.snsproject.model.entity.AlarmEntity;
import com.fastcampus.snsproject.model.entity.CommentEntity;
import com.fastcampus.snsproject.model.entity.LikeEntity;
import com.fastcampus.snsproject.model.entity.PostEntity;
import com.fastcampus.snsproject.model.entity.UserEntity;
import com.fastcampus.snsproject.repository.AlarmEntityRepository;
import com.fastcampus.snsproject.repository.CommentEntityRepository;
import com.fastcampus.snsproject.repository.LikeEntityRepository;
import com.fastcampus.snsproject.repository.PostEntityRepository;
import com.fastcampus.snsproject.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostEntityRepository postEntityRepository;
  private final UserEntityRepository userEntityRepository;
  private final LikeEntityRepository likeEntityRepository;
    private final CommentEntityRepository commentEntityRepository;
    private final AlarmEntityRepository alarmEntityRepository;
    private final AlarmService alarmService;

  @Transactional
  public void create(String title, String body, String userName) {
    // 유저 조회
//        userEntityRepository.findByUserName(userName).orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND,String.format("%s not founded")));

    UserEntity userEntity = getUserEntityOrException(userName);
    postEntityRepository.save(PostEntity.of(title, body, userEntity));
  }

  @Transactional
  public Post modify(String title, String body, String userName, Integer postId) {

//        UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() ->
//            new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)))
//        PostEntity postEntity = postEntityRepository.findById(postId).orElseThrow(() ->
//            new SnsApplicationException(ErrorCode.POST_NOT_FOUND, String.format("%s not founded", postId)));

    UserEntity userEntity = getUserEntityOrException(userName);
    // post 존재 하는지
    PostEntity postEntity = getPostEntityOrException(postId);

    // post 수정 권한 있는지
    if (postEntity.getUser() != userEntity) {
      throw new SnsApplicationException(
          ErrorCode.INVALID_PERMISSION,
          String.format("%s has no permission with %s", userName, postId));
    }

    postEntity.setTitle(title);
    postEntity.setBody(body);

    // save -> updateAt 커밋x
    return Post.fromEntity(postEntityRepository.saveAndFlush(postEntity));
  }

  @Transactional
  public void delete(String userName, Integer postId) {
    UserEntity userEntity = getUserEntityOrException(userName);
    PostEntity postEntity = getPostEntityOrException(postId);

    // post permission
    if (postEntity.getUser() != userEntity) {
      throw new SnsApplicationException(ErrorCode.INVALID_PERMISSION,String.format("%s has no permission with %s", userName, postId));
    }
//        likeEntityRepository.deleteAllByPost(postEntity);
//        commentEntityRepository.deleteAllByPost(postEntity);
    postEntityRepository.delete(postEntity);

  }

  public Page<Post> list(Pageable pageable) {
    return postEntityRepository.findAll(pageable).map(Post::fromEntity);
  }

  public Page<Post> my(String userName, Pageable pageable) {
    UserEntity userEntity = getUserEntityOrException(userName);
    return postEntityRepository.findAllByUser(userEntity, pageable).map(Post::fromEntity);
  }

    @Transactional
    public void like(Integer postId, String userName) {
        // post exist
        PostEntity postEntity = getPostEntityOrException(postId);
        UserEntity userEntity = getUserEntityOrException(userName);

        // 이미 따봉 눌렀는지
        likeEntityRepository.findByUserAndPost(userEntity, postEntity).ifPresent(it -> {
            throw new SnsApplicationException(ErrorCode.ALREADY_LIKED, String.format("userName %s already like post %d", userName, postId));
        });

        // like save
        likeEntityRepository.save(LikeEntity.of(userEntity, postEntity));
        AlarmEntity alarmEntity = alarmEntityRepository.save(AlarmEntity.of(postEntity.getUser(), AlarmType.NEW_LIKE_ON_POST, new AlarmArgs(userEntity.getId(), postEntity.getId())));
        alarmService.send(alarmEntity.getId(), postEntity.getUser().getId());

    }

    public long likeCount(Integer postId) {
        PostEntity postEntity = getPostEntityOrException(postId);
        // count like
        return likeEntityRepository.countByPost(postEntity);
    }

    @Transactional
    public void comment(Integer postId, String userName, String comment) {
        PostEntity postEntity = getPostEntityOrException(postId);
        UserEntity userEntity = getUserEntityOrException(userName);

        //comment save
        commentEntityRepository.save(CommentEntity.of(userEntity, postEntity, comment));
        AlarmEntity alarmEntity = alarmEntityRepository.save(AlarmEntity.of(postEntity.getUser(), AlarmType.NEW_COMMENT_ON_POST, new AlarmArgs(userEntity.getId(), postEntity.getId())));
        alarmService.send(alarmEntity.getId(), postEntity.getUser().getId());
    }

    public Page<Comment> getComments(Integer postId, Pageable pageable) {
        PostEntity postEntity = getPostEntityOrException(postId);
        return commentEntityRepository.findAllByPost(postEntity, pageable).map(Comment::fromEntity);
    }


  // post exist
  private PostEntity getPostEntityOrException(Integer postId) {
    return postEntityRepository.findById(postId).orElseThrow(() ->
        new SnsApplicationException(ErrorCode.POST_NOT_FOUND,
            String.format("%s not founded", postId)));
  }

  // user exist
  private UserEntity getUserEntityOrException(String userName) {
    return userEntityRepository.findByUserName(userName).orElseThrow(() ->
        new SnsApplicationException(ErrorCode.USER_NOT_FOUND,
            String.format("%s not founded", userName)));
  }
}
