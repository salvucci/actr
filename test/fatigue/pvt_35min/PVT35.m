
%% loading the data 
f = fopen('BlockStream.txt');
line = fgetl(f);
b1 = str2num(line);
line = fgetl(f);
b2 = str2num(line);
line = fgetl(f);
b3 = str2num(line);
line = fgetl(f);
b4 = str2num(line);
line = fgetl(f);
b5 = str2num(line);
line = fgetl(f);
b6 = str2num(line);
line = fgetl(f);
b7 = str2num(line);

PVT = load('PVT35min.txt');


U_UT = load('UUT.txt');

BlockLapses = load('BlockLapses.txt');
BlockMeanRT = load('BlockMeanRT.txt');


%% Create figure
figure;

subplot(2,2,1)
% % Create axes
% axes1 = axes('XTickLabel',{'FS','160','180','200','220','240','260','280','300'...
%      ,'320','340','360','380','400','420','440','460','480','Lapses','',''},...
%      'XTickLabelRotation',45,...
%      'XTick',[1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 31 33 35 37 39 40]);
% % Uncomment the following line to preserve the X-limits of the axes
%  xlim(axes1,[0 38]);
% % Uncomment the following line to preserve the Y-limits of the axes

% box(axes1,'on');
% hold(axes1,'on');
% 

% Create multiple lines using matrix input to plot
plot(PVT');
xticks([1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 31 33 35 37 39 40]);
xticklabels({'FS','160','180','200','220','240','260','280','300'...
     ,'320','340','360','380','400','420','440','460','480','Lapses','',''});
xtickangle(45);
%      'XTick',[1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 31 33 35 37 39 40]);
xlim([0 38]);
ylim([-0.005 0.12]);
legend('B1', 'B2','B3','B4','B5','B6','B7');
 

title('PVT Response Time Distribution');
xlabel('Response Time (ms)');
ylabel('Proportion of all Responses');


% %%
% U_UT(:,1) = U_UT(:,1)/60; 
% % Create figure
% figure1 = figure;
% 
% % Create axes
% axes1 = axes('Parent',figure1);
% ylim(axes1,[2 3]);
% box(axes1,'on');
% hold(axes1,'on');
% 
% % Create multiple lines using matrix input to plot
% plot1 = plot(U_UT(:,1),U_UT(:,[2,3]));
% set(plot1(1),'DisplayName','Utility');
% set(plot1(2),'DisplayName','Utility Threshold');
% % Create legend
% legend1 = legend(axes1,'show');
% set(legend1,'FontSize',9);
% title('Trends in Utility/Threshold');
% xlabel('Time on Task (min)');

subplot(2,2,2)
cdfplot(b1);
hold on;

cdfplot(b2);
cdfplot(b3);
cdfplot(b4);
cdfplot(b5);
cdfplot(b6);
cdfplot(b7);
legend('B1', 'B2','B3','B4','B5','B6','B7');
xlim([0 1000]);

%figure for proportion of Lapses in the Blocks
subplot(2,2,3)
plot(BlockLapses);
title('Proportion of Lapses in Blocks');
xticks([1 2 3 4 5 6 7]);
xticklabels({'B1','B2','B3','B4','B5','B6','B7'});
xlim([0 8]);
xlabel('5-min Blocks');

%figure for mean Reaction Time in the Blocks
subplot(2,2,4)
plot (BlockMeanRT);
title('Proportion of False starts in Blocks');
xlabel('5-min Blocks');
