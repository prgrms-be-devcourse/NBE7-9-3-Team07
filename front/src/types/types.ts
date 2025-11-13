export type TagDto = {
  id: number;
  keyword: string;
  createdAt: string;
};

export type PinDto = {
  id: number;
  latitude: number;
  longitude: number;
  content: string;
  userId: number;
  likeCount: number;
  isPublic: boolean;
  createdAt: string;
  modifiedAt: string;
  // 프론트에서 보강
  tags?: string[];        // /api/tags/filter 결과나 /pins/{id}/tags 로 채움
  _tagsLoaded?: boolean;
};

export type LikesStatusDto = {
  isLiked: boolean;
  likeCount: number;
};

export type PinLikedUserDto = {
  id: number;
  userName: string;
};

export type BookmarkDto = {
  id: number;
  pin: PinDto;
  createdAt: string;
};

export type GetFilteredPinResponse = {
  id: number;
  latitude: number;
  longitude: number;
  content: string;
  likeCount: number;
  userNickname: string;
  tags: string[];
  createdAt: string;
  modifiedAt: string;
};
