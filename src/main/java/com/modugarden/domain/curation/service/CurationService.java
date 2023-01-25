package com.modugarden.domain.curation.service;

import com.modugarden.common.error.enums.ErrorMessage;
import com.modugarden.common.error.exception.custom.BusinessException;
import com.modugarden.domain.category.repository.entity.InterestCategory;
import com.modugarden.domain.curation.dto.*;
import com.modugarden.domain.curation.entity.Curation;
import com.modugarden.domain.curation.repository.CurationRepository;
import com.modugarden.domain.like.entity.LikeCuration;
import com.modugarden.domain.like.repository.LikeRepository;
import com.modugarden.domain.user.entity.User;
import com.modugarden.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Service
public class CurationService {
    @Autowired
    private CurationRepository curationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LikeRepository likeRepository;

    @Transactional
    public CurationCreateResponseDto save(CurationCreateRequestDto createRequestDto, MultipartFile file) throws IOException {
        //조건 title 40자 제한, title,link nullable 체크 추가하기.

        // 현재 해당하는 user 정보 가져와서 curation에 저장
//        createRequestDto.setUser(user);

        // 사진 입력
        String projectPath = "C:\\";
        //UUID uuid = UUID.randomUUID();
        String fileName = "_" + file.getOriginalFilename();
        File saveFile = new File(projectPath, fileName);
        //위에 파일객체의 경로와 리네임으로 실제 업로드 하기위해 transferTo()메서드 사용
        file.transferTo(saveFile);
        //db에 파일 저장하기
        createRequestDto.setPreviewImage(fileName);

        // 파일 저장
        Curation curation = createRequestDto.toEntity();

        return new CurationCreateResponseDto(curationRepository.save(curation).getId());
    }

    @Transactional
    public CurationLikeResponseDto createLikes(Long curation_id, Long user_id){
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        User user = userRepository.findById(user_id).orElseThrow(() -> new BusinessException(ErrorMessage.USER_NOT_FOUND));

        boolean isAlreadyLike = likeRepository.findByUserAndCuration(user, curation).isPresent();

        if (!isAlreadyLike) {
            CurationLikeRequestDto curationLikeRequestDto = new CurationLikeRequestDto(user,curation);
            likeRepository.save(curationLikeRequestDto.toEntity());
        }
        return new CurationLikeResponseDto(curation.getId());
    }

    public CurationGetResponseDto get(long id) {
        Curation curation = curationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        return new CurationGetResponseDto(curation);
    }

    public Page<CurationUserGetResponseDto> getUser(long user_id, Pageable pageable) {
        Page<Curation> userCurationList = curationRepository.findAllByUser_Id(user_id, pageable);
        return userCurationList.map(u -> new CurationUserGetResponseDto(u));
    }

    public Slice<CurationSearchResponseDto> search(InterestCategory category, String title, Pageable pageable){
        Slice<Curation> SearchCurationList = curationRepository.findAllByCategoryAndTitleLikeOrderByCreatedDateDesc(category,'%'+title+'%', pageable);
        if(SearchCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);
        return SearchCurationList.map(u -> new CurationSearchResponseDto(u));
    }

    public Slice<CurationSearchResponseDto> getFeed(InterestCategory category, Pageable pageable){
        Slice<Curation> getFeedCurationList = curationRepository.findAllByCategoryOrderByCreatedDateDesc(category, pageable);
        if(getFeedCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);
        return getFeedCurationList.map(u -> new CurationSearchResponseDto(u));
    }

    @Transactional
    public CurationDeleteResponseDto delete(long id){
        Curation curation = curationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION_DELETE));

        curationRepository.delete(curation);
        return new CurationDeleteResponseDto(curation.getId());
    }


}
